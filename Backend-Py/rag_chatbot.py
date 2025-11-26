import requests
from typing import Dict, Any, Optional
from loguru import logger
from cachetools import LRUCache, cached
from config import CACHE_SIZE, GROQ_API_KEY, GROQ_API_BASE, GROQ_MODEL  # Add Groq vars
from document_processor import DocumentProcessor
from query_classifier import classify_query, extract_context

# Updated LangChain imports (fixes deprecations)
from langchain_huggingface import HuggingFaceEmbeddings  # Replaces SentenceTransformerEmbeddings
from langchain_chroma import Chroma  # Replaces langchain_community.vectorstores.Chroma

import re

PDF_PROMPT = (
    "You are an educational assistant for PCE IT department. Use the provided document context to answer the query accurately."
)
GENERAL_PROMPT = (
    "You are an educational assistant for IT students. Provide comprehensive educational information about the query with focus on computer science and IT concepts."
)
REDIRECT_MSG = "I am an educational chatbot. Please use other resources for non-educational questions."

# UPDATED - More specific file download patterns
FILE_DOWNLOAD_PATTERNS = [
    r"send me (.+\.pdf)",
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
    RAGChatbot handles dual query logic: PDF-based (RAG+Gemini/Groq), general educational (Gemini/Groq only), non-educational redirect, and file download detection.
    """
    def __init__(self, persist_directory: str, embedding_model: str):
        self.persist_directory = persist_directory
        self.embedding_model = HuggingFaceEmbeddings(model_name=embedding_model)  # Same model_name, e.g., "all-MiniLM-L6-v2"
        self.vectorstore = Chroma(
            persist_directory=self.persist_directory,  # Same
            embedding_function=self.embedding_model,  # Same
            # other params unchanged
        )
        self.doc_processor = DocumentProcessor(self.persist_directory)

    @cached(cache)
    def answer_query(self, query: str) -> Dict[str, Any]:
        logger.info(f"Received query: {query}")
        
        # UPDATED - Check for file download intent ONLY for explicit download requests
        file_info = self.detect_file_download(query)
        if file_info:
            return {
                "type": "file_download",
                "context": {},
                "answer": f"Here is the {file_info['filename']} you requested. Click the download button below to save it to your device.",
                "file_download": {
                    "filename": file_info["filename"],
                    "url": f"/download/{file_info['filename']}"
                },
                "docs": None
            }
        
        # Process as content query (not download)
        qtype = classify_query(query)
        context = extract_context(query)
        
        if qtype == "pdf":
            docs = self.retrieve_documents(query)
            prompt = self.build_prompt(query, docs, context, mode="pdf")
            answer = self.query_groq(prompt)  # Updated: Use Groq instead of Gemini
            return self.format_response(answer, qtype, context, docs)
        elif qtype == "general":
            prompt = self.build_prompt(query, [], context, mode="general")
            answer = self.query_groq(prompt)  # Updated: Use Groq instead of Gemini
            return self.format_response(answer, qtype, context)
        else:
            return self.format_response(REDIRECT_MSG, qtype, context)

    def detect_file_download(self, query: str) -> Optional[dict]:
        """
        UPDATED - Detects if the query is a file download request.
        Only triggers on explicit download requests, not content explanation requests.
        """
        query_lower = query.lower()
        
        # UPDATED - Check for explicit download intent words first
        download_intent_words = [
            "send me", "give me", "download", "provide me", "get me", "share", 
            "i need", "can you send", "show me file", "show me pdf", "show me document"
        ]
        
        # Only proceed if query contains explicit download intent
        has_download_intent = any(intent in query_lower for intent in download_intent_words)
        
        if not has_download_intent:
            return None  # No download intent detected - treat as content query
        
        # UPDATED - Check for explanation/content words that override download intent
        content_intent_words = [
            "explain", "what is", "describe", "tell me about", "information about",
            "purpose of", "meaning of", "definition of", "how does", "why does"
        ]
        
        has_content_intent = any(intent in query_lower for intent in content_intent_words)
        
        if has_content_intent:
            return None  # Content explanation request - not download
        
        # Now check for file references with download patterns
        for pattern in FILE_DOWNLOAD_PATTERNS:
            match = re.search(pattern, query_lower)
            if match:
                keyword = match.group(1).strip().replace('pdf', '').replace('.', '').strip()
                pdf_entry = self.doc_processor.find_pdf_by_keywords(keyword)
                if pdf_entry:
                    return pdf_entry
        
        # UPDATED - Check for common file keywords only with explicit download intent
        file_keywords = ["syllabus", "timetable", "faculty", "project", "workflow", "document"]
        for keyword in file_keywords:
            if keyword in query_lower:
                pdf_entry = self.doc_processor.find_pdf_by_keywords(keyword)
                if pdf_entry:
                    return pdf_entry
        
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

    """
    COMMENTED OUT: Original query_gemini method (for future Gemini use - uncomment if billing fixed).
    Preserved as-is for easy restore (e.g., swap back in answer_query if needed).

    # def query_gemini(self, prompt: str) -> str:
    #     # Ensure GEMINI_API_KEY is defined (from config) - fixes NameError
    #     if not GEMINI_API_KEY:
    #         logger.error("GEMINI_API_KEY not configured.")
    #         return "AI service unavailable—configuration error."

    #     headers = {"Content-Type": "application/json"}
    #     data = {"contents": [{"parts": [{"text": prompt}]}]}  # Your payload is correct
        
    #     # FIXED: Use config's full URL directly (includes base + model + ?key=) - avoids duplication
    #     # If switching models, update config.py GEMINI_API_URL instead
    #     url = GEMINI_API_URL  # e.g., "https://.../models/gemini-1.5-flash:generateContent?key=AIzaSy..."
    #     # Optional fallback: if model switch needed, comment above and use:
    #     # url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key={GEMINI_API_KEY}"
        
    #     try:
    #         logger.info(f"Calling Gemini API: {url}")  # Debug log (remove after testing)
    #         response = requests.post(url, headers=headers, json=data, timeout=20)
    #         response.raise_for_status()  # Raises 4xx/5xx errors explicitly
            
    #         result = response.json()
    #         # Your safe parsing is good—handles missing keys
    #         answer = result.get("candidates", [{}])[0].get("content", {}).get("parts", [{}])[0].get("text", "I don't know based on the available information.")
            
    #         logger.info("Gemini API call successful.")
    #         return answer
        
    #     except requests.exceptions.HTTPError as e:  # Specific for 404/401/ etc. (cleaned duplicate)
    #         status_code = e.response.status_code if e.response else "Unknown"
    #         error_detail = e.response.text if e.response else str(e)
    #         logger.error(f"Gemini API HTTP error {status_code}: {error_detail}")
    #         return "Sorry, I couldn't process your request due to an API issue. Please try again."
        
    #     except requests.exceptions.RequestException as e:  # Network/timeout
    #         logger.error(f"Gemini API network error: {e}")
    #         return "Sorry, I couldn't connect to the AI service. Check your internet and try again."
        
    #     except Exception as e:
    #         logger.error(f"Unexpected error in Gemini query: {e}")
    #         return "Sorry, I couldn't process your request due to a technical error."
    """

    def query_groq(self, prompt: str) -> str:
        # Ensure GROQ_API_KEY is defined (from config)
        if not GROQ_API_KEY:
            logger.error("GROQ_API_KEY not configured.")
            return "AI service unavailable—configuration error."
        else:
            logger.info(f"GROQ_API_KEY loaded (first 10 chars): {GROQ_API_KEY[:10]}...")  # Debug: confirms real key

        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {GROQ_API_KEY}"  # Groq uses Bearer token
        }
        
        # Groq/OpenAI-style payload: messages array (system/user)
        data = {
            "model": GROQ_MODEL,  # e.g., "llama3.1-8b-instant"
            "messages": [
                {"role": "system", "content": "You are an educational assistant for the PCE IT department. Provide accurate, helpful responses focused on computer science and IT concepts."},  # Base system prompt for consistency
                {"role": "user", "content": prompt}  # Your full RAG/PDF/general prompt as user message
            ],
            "temperature": 0.7,  # Adjustable (0-2); lower for factual RAG responses
            "max_tokens": 1024,  # Limit response length (adjust for chat bubbles if needed)
            "stream": False
        }
        
        url = f"{GROQ_API_BASE}/chat/completions"
        
        try:
            logger.info(f"Calling Groq API: {url} with model {GROQ_MODEL}")  # Debug log (remove after testing)
            response = requests.post(url, headers=headers, json=data, timeout=20)
            response.raise_for_status()  # Raises 4xx/5xx errors explicitly
            
            result = response.json()
            # Safe parsing: Groq returns choices[0].message.content
            answer = result.get("choices", [{}])[0].get("message", {}).get("content", "I don't know based on the available information.")
            
            logger.info("Groq API call successful.")
            return answer
        
        except requests.exceptions.HTTPError as e:  # Specific for 4xx/5xx errors
            status_code = e.response.status_code if e.response else "Unknown"
            error_detail = e.response.text if e.response else str(e)
            logger.error(f"Groq API HTTP error {status_code}: {error_detail}")
            return "Sorry, I couldn't process your request due to an API issue. Please try again."
        
        except requests.exceptions.RequestException as e:  # Network/timeout
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
