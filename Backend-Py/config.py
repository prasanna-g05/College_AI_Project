import os
from dotenv import load_dotenv

load_dotenv()

# Existing Gemini (keep commented if not using)
# GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
# GEMINI_API_URL = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key={GEMINI_API_KEY}"

# Groq (free, open-source) - No hardcoded key; requires .env
GROQ_API_KEY = os.getenv("GROQ_API_KEY")  # Loads from .env only; None if missing
GROQ_API_BASE = "https://api.groq.com/openai/v1"
GROQ_MODEL = "llama-3.1-8b-instant"  # With hyphens (fast, free model)

CHROMA_DB_DIR = "chroma_db"
EMBEDDING_MODEL = "all-MiniLM-L6-v2"
CACHE_SIZE = 100
