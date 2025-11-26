import os
from typing import List, Dict, Optional
from langchain_community.document_loaders import PyPDFLoader  # Keep - not deprecated
from langchain.text_splitter import RecursiveCharacterTextSplitter  # Keep
# Updated imports (fixes deprecations)
from langchain_chroma import Chroma  # Replaces langchain_community.vectorstores.Chroma
from langchain_huggingface import HuggingFaceEmbeddings  # Replaces SentenceTransformerEmbeddings
from loguru import logger
from config import CHROMA_DB_DIR, EMBEDDING_MODEL
import json
import shutil

PDF_METADATA_FILE = os.path.join(CHROMA_DB_DIR, "pdf_metadata.json")
PDF_STORAGE_DIR = os.path.join(CHROMA_DB_DIR, "pdf_files")

class DocumentProcessor:
    """
    Handles PDF document processing, chunking, embedding, storage in ChromaDB, and PDF file/metadata management.
    """
    def __init__(self, persist_directory: str = CHROMA_DB_DIR):
        self.persist_directory = persist_directory
        self.embedding_model = HuggingFaceEmbeddings(model_name=EMBEDDING_MODEL)  # Updated class - fixes warning
        self.text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=800,
            chunk_overlap=100,
            separators=["\n\n", "\n", ".", "!", "?", ",", " ", ""]
        )
        self.vectorstore = None
        os.makedirs(PDF_STORAGE_DIR, exist_ok=True)
        logger.info(f"DocumentProcessor initialized with persist_directory={persist_directory}")

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
            docs = self.load_pdf(path)
            for doc in docs:
                chunks = self.text_splitter.split_text(doc.page_content)
                for chunk in chunks:
                    all_chunks.append({"content": chunk, "metadata": doc.metadata})
            # Store PDF and metadata
            self.save_pdf_with_metadata(path)
        logger.info(f"Total chunks created: {len(all_chunks)}")
        if all_chunks:
            self.save_to_chromadb(all_chunks)
        return len(all_chunks)

    def save_to_chromadb(self, chunks: List[dict]):
        try:
            texts = [chunk["content"] for chunk in chunks]
            metadatas = [chunk["metadata"] for chunk in chunks]
            self.vectorstore = Chroma.from_texts(  # Updated class
                texts=texts,
                embedding=self.embedding_model,  # Updated (HuggingFace)
                metadatas=metadatas,
                persist_directory=self.persist_directory
            )
            self.vectorstore.persist()
            logger.info(f"Saved {len(texts)} chunks to ChromaDB at {self.persist_directory}")
        except Exception as e:
            logger.error(f"Error saving to ChromaDB: {e}")

    def get_vectorstore(self):
        if self.vectorstore is not None:
            return self.vectorstore
        try:
            self.vectorstore = Chroma(  # Updated class
                embedding_function=self.embedding_model,  # Updated (HuggingFace)
                persist_directory=self.persist_directory
            )
            logger.info("Loaded existing ChromaDB vectorstore.")
            return self.vectorstore
        except Exception as e:
            logger.error(f"Error loading ChromaDB vectorstore: {e}")
            return None

    def save_pdf_with_metadata(self, pdf_path: str):
        """
        Save the original PDF to storage and update metadata for keyword/file matching.
        """
        try:
            filename = os.path.basename(pdf_path)
            dest_path = os.path.join(PDF_STORAGE_DIR, filename)
            shutil.copy2(pdf_path, dest_path)
            # Extract keywords from filename (simple split, can be improved)
            keywords = [w.lower() for w in os.path.splitext(filename)[0].replace('_', ' ').replace('-', ' ').split()]
            metadata = self.load_pdf_metadata()
            metadata[filename] = {
                "filename": filename,
                "path": dest_path,
                "keywords": keywords
            }
            self.save_pdf_metadata(metadata)
            logger.info(f"Saved PDF {filename} with keywords {keywords}")
        except Exception as e:
            logger.error(f"Error saving PDF and metadata: {e}")

    def load_pdf_metadata(self) -> Dict[str, dict]:
        if os.path.exists(PDF_METADATA_FILE):
            try:
                with open(PDF_METADATA_FILE, "r", encoding="utf-8") as f:
                    return json.load(f)
            except Exception as e:
                logger.error(f"Error loading PDF metadata: {e}")
        return {}

    def save_pdf_metadata(self, metadata: Dict[str, dict]):
        try:
            with open(PDF_METADATA_FILE, "w", encoding="utf-8") as f:
                json.dump(metadata, f, indent=2)
        except Exception as e:
            logger.error(f"Error saving PDF metadata: {e}")

    def find_pdf_by_keywords(self, query: str) -> Optional[dict]:
        """
        Fuzzy match query to PDF metadata keywords. Returns best match or None.
        """
        import difflib
        metadata = self.load_pdf_metadata()
        query_words = [w.lower() for w in query.replace('_', ' ').replace('-', ' ').split()]
        best_score = 0
        best_entry = None
        for entry in metadata.values():
            score = len(set(query_words) & set(entry["keywords"]))
            if score > best_score:
                best_score = score
                best_entry = entry
        # Fallback: fuzzy match if no direct overlap
        if not best_entry and metadata:
            all_keywords = [k for entry in metadata.values() for k in entry["keywords"]]
            close = difflib.get_close_matches(' '.join(query_words), all_keywords, n=1, cutoff=0.7)
            if close:
                for entry in metadata.values():
                    if close[0] in entry["keywords"]:
                        return entry
        return best_entry

    def get_pdf_file(self, filename: str) -> Optional[str]:
        """
        Return the path to the stored PDF file if it exists.
        """
        metadata = self.load_pdf_metadata()
        entry = metadata.get(filename)
        if entry and os.path.exists(entry["path"]):
            return entry["path"]
        return None
