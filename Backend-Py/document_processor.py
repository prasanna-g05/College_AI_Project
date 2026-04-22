import os
import re
import json
from typing import List, Dict, Optional, Any
from langchain_community.document_loaders import PyPDFLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_chroma import Chroma
from langchain_huggingface import HuggingFaceEmbeddings
from loguru import logger
from config import CHROMA_DB_DIR, EMBEDDING_MODEL

PDF_METADATA_FILE = os.path.join(CHROMA_DB_DIR, "pdf_metadata.json")


class DocumentProcessor:
    """
    Handles PDF document processing, chunking, embedding, storage in ChromaDB,
    and PDF metadata management using the uploaded file as the single source of truth.
    """

    def __init__(self, persist_directory: str = CHROMA_DB_DIR):
        self.persist_directory = persist_directory
        self.embedding_model = HuggingFaceEmbeddings(model_name=EMBEDDING_MODEL)
        self.text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=800,
            chunk_overlap=100,
            separators=["\n\n", "\n", ".", "!", "?", ",", " ", ""]
        )
        self.vectorstore = None
        os.makedirs(self.persist_directory, exist_ok=True)
        os.makedirs(os.path.dirname(PDF_METADATA_FILE), exist_ok=True)
        logger.info(f"DocumentProcessor initialized with persist_directory={persist_directory}")

    def normalize_text(self, text: str) -> str:
        text = text.lower().strip()
        text = text.replace(".pdf", "")
        text = text.replace("_", " ").replace("-", " ")
        text = re.sub(r"[^\w\s]", " ", text)
        text = re.sub(r"\s+", " ", text).strip()

        replacements = {
            "semester": "sem",
            "semesters": "sem",
            "1st": "1",
            "2nd": "2",
            "3rd": "3",
            "4th": "4",
            "5th": "5",
            "6th": "6",
            "7th": "7",
            "8th": "8",
            "first": "1",
            "second": "2",
            "third": "3",
            "fourth": "4",
            "fifth": "5",
            "sixth": "6",
            "seventh": "7",
            "eighth": "8",
            "i": "1",
            "ii": "2",
            "iii": "3",
            "iv": "4",
            "v": "5",
            "vi": "6",
            "vii": "7",
            "viii": "8",
            "note": "notes",
        }

        words = []
        for word in text.split():
            words.append(replacements.get(word, word))

        return " ".join(words)

    def tokenize_text(self, text: str) -> List[str]:
        normalized = self.normalize_text(text)
        return [token for token in normalized.split() if token]

    def score_pdf_match(self, query_tokens: List[str], file_tokens: List[str]) -> float:
        query_set = set(query_tokens)
        file_set = set(file_tokens)
        if not query_set or not file_set:
            return 0.0

        exact_overlap = len(query_set & file_set)
        score = float(exact_overlap)

        if "unit" in query_set and "unit" in file_set:
            score += 1.5
        if "sem" in query_set and "sem" in file_set:
            score += 1.5
        if "notes" in query_set and "notes" in file_set:
            score += 1.0
        if "syllabus" in query_set and "syllabus" in file_set:
            score += 1.0

        query_numbers = {t for t in query_set if t.isdigit()}
        file_numbers = {t for t in file_set if t.isdigit()}
        if query_numbers:
            matched_numbers = query_numbers & file_numbers
            if matched_numbers:
                score += len(matched_numbers) * 3.0
            elif file_numbers:
                score -= 3.0
        return score

    def load_pdf(self, pdf_path: str):
        try:
            loader = PyPDFLoader(pdf_path)
            docs = loader.load()
            logger.info(f"Loaded {len(docs)} pages from {pdf_path}")
            return docs
        except Exception as e:
            logger.error(f"Error loading PDF {pdf_path}: {e}")
            return []

    def process_documents(self, pdf_paths: List[str]) -> int:
        all_chunks = []

        for path in pdf_paths:
            abs_path = self._normalize_file_path(path)
            docs = self.load_pdf(abs_path)

            for doc in docs:
                chunks = self.text_splitter.split_text(doc.page_content)
                for chunk in chunks:
                    all_chunks.append({"content": chunk, "metadata": doc.metadata})

            self.save_pdf_metadata_entry(abs_path)

        logger.info(f"Total chunks created: {len(all_chunks)}")

        if all_chunks:
            self.save_to_chromadb(all_chunks)

        return len(all_chunks)

    def save_to_chromadb(self, chunks: List[dict]):
        try:
            texts = [chunk["content"] for chunk in chunks]
            metadatas = [chunk["metadata"] for chunk in chunks]
            self.vectorstore = Chroma.from_texts(
                texts=texts,
                embedding=self.embedding_model,
                metadatas=metadatas,
                persist_directory=self.persist_directory
            )
            logger.info(f"Saved {len(texts)} chunks to ChromaDB at {self.persist_directory}")
        except Exception as e:
            logger.error(f"Error saving to ChromaDB: {e}")

    def get_vectorstore(self):
        if self.vectorstore is not None:
            return self.vectorstore

        try:
            self.vectorstore = Chroma(
                embedding_function=self.embedding_model,
                persist_directory=self.persist_directory
            )
            logger.info("Loaded existing ChromaDB vectorstore.")
            return self.vectorstore
        except Exception as e:
            logger.error(f"Error loading ChromaDB vectorstore: {e}")
            return None

    def _normalize_file_path(self, pdf_path: str) -> str:
        return os.path.abspath(os.path.normpath(os.path.expanduser(pdf_path)))

    def save_pdf_metadata_entry(self, pdf_path: str):
        """
        Save metadata for the uploaded PDF without copying it elsewhere.
        The uploaded file path remains the single source of truth.
        """
        try:
            normalized_path = self._normalize_file_path(pdf_path)

            if not os.path.exists(normalized_path):
                logger.warning(f"PDF path does not exist, skipping metadata save: {normalized_path}")
                return

            filename = os.path.basename(normalized_path)
            keywords = self.tokenize_text(os.path.splitext(filename)[0])

            metadata = self.load_pdf_metadata()
            metadata[filename] = {
                "filename": filename,
                "path": normalized_path,
                "keywords": keywords
            }
            self.save_pdf_metadata(metadata)
            logger.info(f"Saved PDF metadata for {filename} with path {normalized_path}")
        except Exception as e:
            logger.error(f"Error saving PDF metadata: {e}")

    def delete_pdf_metadata_entry(self, filename: str):
        try:
            metadata = self.load_pdf_metadata()
            if filename in metadata:
                del metadata[filename]
                self.save_pdf_metadata(metadata)
        except Exception as e:
            logger.error(f"Error deleting PDF metadata: {e}")

    def load_pdf_metadata(self) -> Dict[str, dict]:
        if os.path.exists(PDF_METADATA_FILE):
            try:
                with open(PDF_METADATA_FILE, "r", encoding="utf-8") as f:
                    data = json.load(f)
                    if isinstance(data, dict):
                        return data
            except Exception as e:
                logger.error(f"Error loading PDF metadata: {e}")
        return {}

    def save_pdf_metadata(self, metadata: Dict[str, dict]):
        try:
            os.makedirs(os.path.dirname(PDF_METADATA_FILE), exist_ok=True)
            with open(PDF_METADATA_FILE, "w", encoding="utf-8") as f:
                json.dump(metadata, f, indent=2, ensure_ascii=False)
        except Exception as e:
            logger.error(f"Error saving PDF metadata: {e}")

    def find_pdf_by_keywords(self, query: str) -> Dict[str, Any]:
        """
        Returns either:
        - {"type": "single", "entry": {...}}
        - {"type": "multiple", "entries": [{...}, {...}]}
        - {"type": "none", "entries": []}
        """
        metadata = self.load_pdf_metadata()
        if not metadata:
            return {"type": "none", "entries": []}

        query_tokens = self.tokenize_text(query)
        scored_matches = []

        for entry in metadata.values():
            file_tokens = entry.get("keywords", [])
            score = self.score_pdf_match(query_tokens, file_tokens)
            if score > 0:
                scored_matches.append({
                    "entry": entry,
                    "score": score
                })

        scored_matches.sort(key=lambda item: item["score"], reverse=True)

        if not scored_matches:
            return {"type": "none", "entries": []}

        if len(scored_matches) == 1:
            return {
                "type": "single",
                "entry": scored_matches[0]["entry"]
            }

        top_score = scored_matches[0]["score"]
        strong_matches = [
            item for item in scored_matches
            if item["score"] >= max(2.0, top_score * 0.85)
        ]

        if len(strong_matches) == 1:
            return {
                "type": "single",
                "entry": strong_matches[0]["entry"]
            }

        return {
            "type": "multiple",
            "entries": [item["entry"] for item in strong_matches[:5]]
        }

    def get_pdf_file(self, filename: str) -> Optional[str]:
        """
        Return the path to the uploaded PDF file if it exists.
        """
        metadata = self.load_pdf_metadata()
        entry = metadata.get(filename)
        if not entry:
            return None

        path = entry.get("path")
        if not path:
            return None

        normalized_path = self._normalize_file_path(path)
        if os.path.exists(normalized_path) and os.path.isfile(normalized_path):
            return normalized_path

        return None
