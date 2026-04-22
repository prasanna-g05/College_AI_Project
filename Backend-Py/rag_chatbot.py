import requests
import re
from typing import Dict, Any, Optional
from loguru import logger
from cachetools import LRUCache, cached
from config import CACHE_SIZE, GROQ_API_KEY, GROQ_API_BASE, GROQ_MODEL
from document_processor import DocumentProcessor
from query_classifier import classify_query, extract_context
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_chroma import Chroma


PDF_PROMPT = (
    "You are an educational assistant for PCE IT department. Use the provided document context to answer the query accurately."
)
GENERAL_PROMPT = (
    "You are an educational assistant for IT students. Provide comprehensive educational information about the query with focus on computer science and IT concepts."
)
REDIRECT_MSG = "I am an educational chatbot. Please use other resources for non-educational questions."


FILE_DOWNLOAD_PATTERNS = [
    r"send me (.+\.pdf)",
    r"send (.+\.pdf)",
    r"give me (.+\.pdf)",
    r"send me (.+)",
    r"give me (.+)",
    r"download (.+)",
    r"provide me (.+)",
    r"get me (.+)",
    r"share (.+)",
    r"i need (.+?) (file|pdf|document)",
    r"can you send (.+)",
    r"show me (file|pdf|document)"
]


cache = LRUCache(maxsize=CACHE_SIZE)


class RAGChatbot:
    """
    RAGChatbot handles dual query logic: PDF-based (RAG+Groq), general educational,
    non-educational redirect, and file download detection.
    """
    def __init__(self, persist_directory: str, embedding_model: str):
        self.persist_directory = persist_directory
        self.embedding_model = HuggingFaceEmbeddings(model_name=embedding_model)
        self.vectorstore = Chroma(
            persist_directory=self.persist_directory,
            embedding_function=self.embedding_model,
        )
        self.doc_processor = DocumentProcessor(self.persist_directory)

    @cached(cache)
    def answer_query(self, query: str) -> Dict[str, Any]:
        logger.info(f"Received query: {query}")

        file_info = self.detect_file_download(query)
        if file_info:
            if file_info["type"] == "single":
                entry = file_info["entry"]
                return {
                    "type": "file_download",
                    "context": {},
                    "answer": f"Here is the {entry['filename']} you requested. Click the download button below to save it to your device.",
                    "file_download": {
                        "filename": entry["filename"],
                        "url": f"/download/{entry['filename']}"
                    },
                    "docs": None
                }

            if file_info["type"] == "multiple":
                entries = file_info["entries"]
                matching_files = [
                    {
                        "filename": entry["filename"],
                        "download_url": f"/download/{entry['filename']}"
                    }
                    for entry in entries
                ]
                return {
                    "type": "file_selection",
                    "context": {},
                    "answer": "I found multiple matching files. Please choose the one you want to download.",
                    "matching_files": matching_files,
                    "docs": None
                }

        qtype = classify_query(query)
        context = extract_context(query)

        if qtype == "pdf":
            docs = self.retrieve_documents(query)
            prompt = self.build_prompt(query, docs, context, mode="pdf")
            answer = self.query_groq(prompt)
            return self.format_response(answer, qtype, context, docs)
        elif qtype == "general":
            prompt = self.build_prompt(query, [], context, mode="general")
            answer = self.query_groq(prompt)
            return self.format_response(answer, qtype, context)
        else:
            return self.format_response(REDIRECT_MSG, qtype, context)

    def detect_file_download(self, query: str) -> Optional[dict]:
        """
        Detects if the query is a file download request.
        Only triggers on explicit download requests, not content explanation requests.
        Returns:
        - {"type": "single", "entry": {...}}
        - {"type": "multiple", "entries": [{...}]}
        - None
        """
        query_lower = query.lower().strip()

        content_intent_patterns = [
            r"\bexplain\b",
            r"\bwhat is\b",
            r"\bdescribe\b",
            r"\btell me about\b",
            r"\binformation about\b",
            r"\bpurpose of\b",
            r"\bmeaning of\b",
            r"\bdefinition of\b",
            r"\bhow does\b",
            r"\bwhy does\b"
        ]
        if any(re.search(p, query_lower) for p in content_intent_patterns):
            return None

        download_intent_patterns = [
            r"\bsend me\b",
            r"\bsend\b",
            r"\bgive me\b",
            r"\bdownload\b",
            r"\bprovide me\b",
            r"\bget me\b",
            r"\bshare\b",
            r"\bi need\b",
            r"\bcan you send\b",
            r"\bshow me file\b",
            r"\bshow me pdf\b",
            r"\bshow me document\b"
        ]
        if not any(re.search(p, query_lower) for p in download_intent_patterns):
            return None

        for pattern in FILE_DOWNLOAD_PATTERNS:
            match = re.search(pattern, query_lower)
            if match:
                keyword = match.group(1).strip()
                keyword = re.sub(r"\b(pdf|file|document)\b", "", keyword)
                keyword = re.sub(r"\s+", " ", keyword).strip()
                pdf_result = self.doc_processor.find_pdf_by_keywords(keyword)
                if pdf_result["type"] in ("single", "multiple"):
                    return pdf_result

        file_keywords = [
            "syllabus",
            "timetable",
            "faculty",
            "project",
            "workflow",
            "document",
            "notes"
        ]

        for keyword in file_keywords:
            if re.search(rf"\b{re.escape(keyword)}\b", query_lower):
                pdf_result = self.doc_processor.find_pdf_by_keywords(query_lower)
                if pdf_result["type"] in ("single", "multiple"):
                    return pdf_result

        return None

    def retrieve_documents(self, query: str, k: int = 4):
        try:
            results = self.vectorstore.similarity_search(query, k=k)
            logger.info(f"Retrieved {len(results)} relevant chunks for query.")
            return results
        except Exception as e:
            logger.error(f"Error during similarity search: {e}")
            return []

    def build_prompt(self, query: str, docs, context: dict, mode: str) -> str:
        context_str = "\n".join([doc.page_content for doc in docs]) if docs else ""
        context_info = ""

        if context.get("year"):
            context_info += f"Year: {context['year']}\n"
        if context.get("semester"):
            context_info += f"Semester: {context['semester']}\n"
        if context.get("course"):
            context_info += f"Course: {context['course']}\n"

        if mode == "pdf":
            prompt = f"{PDF_PROMPT}\nContext:\n{context_str}\n{context_info}User Query: {query}\nAnswer: "
        else:
            prompt = f"{GENERAL_PROMPT}\n{context_info}User Query: {query}\nAnswer: "

        return prompt

    def query_groq(self, prompt: str) -> str:
        if not GROQ_API_KEY:
            logger.error("GROQ_API_KEY not configured.")
            return "AI service unavailable—configuration error."
        else:
            logger.info(f"GROQ_API_KEY loaded (first 10 chars): {GROQ_API_KEY[:10]}...")

        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {GROQ_API_KEY}"
        }

        data = {
            "model": GROQ_MODEL,
            "messages": [
                {
                    "role": "system",
                    "content": "You are an educational assistant for the PCE IT department. Provide accurate, helpful responses focused on computer science and IT concepts."
                },
                {"role": "user", "content": prompt}
            ],
            "temperature": 0.7,
            "max_tokens": 1024,
            "stream": False
        }

        url = f"{GROQ_API_BASE}/chat/completions"

        try:
            logger.info(f"Calling Groq API: {url} with model {GROQ_MODEL}")
            response = requests.post(url, headers=headers, json=data, timeout=20)
            response.raise_for_status()

            result = response.json()
            answer = result.get("choices", [{}])[0].get("message", {}).get("content", "I don't know based on the available information.")

            logger.info("Groq API call successful.")
            return answer

        except requests.exceptions.HTTPError as e:
            status_code = e.response.status_code if e.response else "Unknown"
            error_detail = e.response.text if e.response else str(e)
            logger.error(f"Groq API HTTP error {status_code}: {error_detail}")
            return "Sorry, I couldn't process your request due to an API issue. Please try again."

        except requests.exceptions.RequestException as e:
            logger.error(f"Groq API network error: {e}")
            return "Sorry, I couldn't connect to the AI service. Check your internet and try again."

        except Exception as e:
            logger.error(f"Unexpected error in Groq query: {e}")
            return "Sorry, I couldn't process your request due to a technical error."

    def format_response(self, answer: str, qtype: str, context: dict, docs: Optional[list] = None) -> Dict[str, Any]:
        return {
            "type": qtype,
            "context": context,
            "answer": answer,
            "docs": [doc.page_content for doc in docs] if docs else None
        }

    def process_pdfs(self, pdf_paths):
        return self.doc_processor.process_documents(pdf_paths)
