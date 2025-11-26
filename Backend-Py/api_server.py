# api_server.py
# Purpose: Main FastAPI app—serves RAG endpoints (/upload, /chat, /download) and auth (/auth/register, /auth/login).
# Integrates rag_chatbot.py (RAGChatbot class with Groq/PDF/general logic), database.py (DB setup), auth.py (user CRUD).
# CORS for Android; static files for /download. Instantiates RAGChatbot for full RAG functionality.
# Run: uvicorn api_server:app --reload; access /docs for Swagger. Assumes config.py, chroma_db/ dir.
from pydantic import BaseModel
from typing import Optional
from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
import os
from typing import List
import shutil
from dotenv import load_dotenv  # Add if using .env for keys; load_dotenv()

# Load env vars (if .env exists)
load_dotenv()

# Auth and DB imports (new—work as-is)
from auth import auth_router  # /auth/register, /auth/login
from database import create_tables  # DB startup

# RAG import (precise for your class-based rag_chatbot.py)
try:
    from rag_chatbot import RAGChatbot
    # Instantiate with defaults from your code (adjust persist_directory/embedding_model if needed)
    chatbot = RAGChatbot(persist_directory="chroma_db", embedding_model="all-MiniLM-L6-v2")
    RAG_AVAILABLE = True
    print("✅ RAGChatbot instantiated successfully.")
except ImportError as e:
    print(f"❌ RAG import error: {e}. Install deps: pip install langchain-huggingface langchain-chroma etc.")
    RAG_AVAILABLE = False
except Exception as e:
    print(f"❌ RAG init error (e.g., missing config.py or chroma_db): {e}")
    RAG_AVAILABLE = False

# Create FastAPI app
app = FastAPI(title="PCE IT Assistant API", version="2.0", description="RAG Chatbot (Groq/Chroma) with User Auth")

# CORS (for Android)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Prod: Limit e.g., ["http://10.0.2.2"]
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Static files for /download
if not os.path.exists("uploaded_pdfs"):
    os.makedirs("uploaded_pdfs")
app.mount("/files", StaticFiles(directory="uploaded_pdfs"), name="files")

# Include auth router
app.include_router(auth_router, prefix="/auth", tags=["Auth"])

class ChatRequest(BaseModel):
    query: str
    erp_number: Optional[str] = None
    semester: Optional[str] = None
    year: Optional[str] = None

# Startup: Create DB tables
@app.on_event("startup")
def startup_event():
    create_tables()
    print("✅ DB setup complete. RAG status:", "Available" if RAG_AVAILABLE else "Disabled")

# RAG: /upload (saves PDFs, processes to Chroma via RAGChatbot.process_pdfs)
@app.post("/upload")
async def upload_files(files: List[UploadFile] = File(...)):
    if not files:
        raise HTTPException(status_code=400, detail="No files provided.")
    if not RAG_AVAILABLE:
        raise HTTPException(status_code=503, detail="RAG service unavailable.")
    
    pdf_paths = []
    for file in files:
        if file.content_type != "application/pdf":
            raise HTTPException(status_code=400, detail="Only PDFs allowed.")
        file_path = f"uploaded_pdfs/{file.filename}"
        with open(file_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
        pdf_paths.append(file_path)
    
    # Process via RAGChatbot (calls DocumentProcessor internally)
    try:
        result = chatbot.process_pdfs(pdf_paths)
        return {"message": f"{len(pdf_paths)} PDFs uploaded and processed to Chroma.", "result": result}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Processing failed: {str(e)}")

# RAG: /chat (calls RAGChatbot.answer_query—handles classification, Groq, file detect)
@app.post("/chat")
async def chat_endpoint(request: ChatRequest):
    query = request.query
    erp_number = request.erp_number
    semester = request.semester
    year = request.year
    if not query:
        raise HTTPException(status_code=400, detail="Query required.")
    if not RAG_AVAILABLE:
        raise HTTPException(status_code=503, detail="RAG service unavailable.")
    
    response = chatbot.answer_query(query)
    if erp_number and "answer" in response:
        response["answer"] += f" (Personalized for ERP: {erp_number})"
    return response


# RAG: /download (serves PDFs)
@app.get("/download/{filename}")
async def download_file(filename: str):
    file_path = f"uploaded_pdfs/{filename}"
    if os.path.exists(file_path):
        return FileResponse(file_path, media_type="application/pdf", filename=filename)
    raise HTTPException(status_code=404, detail="File not found.")

# Old mock /login (commented—use /auth/login. Uncomment if needed for Android transition)
"""
@app.post("/login")
def login_mock(erp_request: dict):
    erp = erp_request.get("erp_number")
    # Your original mock logic here if needed
    return {"success": True, "student": {"erp_number": erp, ...}}
"""

# Root health check
@app.get("/")
def root():
    return {"message": "PCE IT Assistant API v2.0 - Full RAG (Groq/Chroma) + Auth Ready. /docs for testing."}

# Health (for Android)
@app.get("/health")
def health_check():
    return {"status": "healthy", "rag_available": RAG_AVAILABLE, "version": "2.0"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
