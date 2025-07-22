import os
import time
from typing import List, Optional
from fastapi import FastAPI, File, UploadFile, HTTPException, Request, Response
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, FileResponse
from pydantic import BaseModel
from loguru import logger
from rag_chatbot import RAGChatbot
from config import CHROMA_DB_DIR, EMBEDDING_MODEL
from document_processor import DocumentProcessor

app = FastAPI(title="PCE IT Department Educational RAG Chatbot API",
              description="REST API for educational chatbot with RAG and Gemini integration.",
              version="1.0.0")

# CORS for Android/Frontend integration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Change to specific domains in production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

chatbot = RAGChatbot(CHROMA_DB_DIR, EMBEDDING_MODEL)
doc_processor = DocumentProcessor(CHROMA_DB_DIR)

# --- Pydantic Models ---
class ChatRequest(BaseModel):
    query: str
    year: Optional[str] = None
    semester: Optional[str] = None
    course: Optional[str] = None

class ChatResponse(BaseModel):
    type: str
    context: dict
    answer: str
    docs: Optional[List[str]] = None
    file_download: Optional[dict] = None

class UploadResponse(BaseModel):
    message: str
    num_chunks: int

class StatsResponse(BaseModel):
    queries_answered: int
    uptime_seconds: float

# --- NEW: Login Models ---
class LoginRequest(BaseModel):
    erp_number: str

class StudentData(BaseModel):
    erpNumber: str
    name: str
    department: str
    year: str
    semester: str

class LoginResponse(BaseModel):
    success: bool
    message: str
    student: Optional[StudentData] = None

# --- State ---
start_time = time.time()
query_count = 0

# --- Endpoints ---
@app.get("/health", tags=["Utility"])
def health():
    """Health check endpoint."""
    return {"status": "ok"}

# --- NEW: Login Endpoint ---
@app.post("/login", response_model=LoginResponse, tags=["Authentication"])
async def login_student(request: LoginRequest):
    """
    Authenticate student with ERP number for PCE IT Department.
    """
    try:
        # Validate ERP number format (9 digits)
        if len(request.erp_number) != 9 or not request.erp_number.isdigit():
            raise HTTPException(
                status_code=400, 
                detail="Invalid ERP number format. Must be 9 digits."
            )
        
        # Extract year from ERP number (first 2 digits represent admission year)
        admission_year = int(request.erp_number[:2])
        current_year = time.localtime().tm_year % 100
        
        # Calculate academic year based on admission year
        year_diff = current_year - admission_year
        if year_diff <= 0:
            academic_year = "1st Year"
            semester = "1st Semester"
        elif year_diff == 1:
            academic_year = "2nd Year"
            semester = "3rd Semester"
        elif year_diff == 2:
            academic_year = "3rd Year"
            semester = "5th Semester"
        elif year_diff == 3:
            academic_year = "4th Year"
            semester = "7th Semester"
        else:
            academic_year = "Graduate"
            semester = "Completed"
        
        # Generate student data
        student_data = StudentData(
            erpNumber=request.erp_number,
            name=f"Student {request.erp_number[-3:]}",  # Use last 3 digits
            department="Information Technology",
            year=academic_year,
            semester=semester
        )
        
        logger.info(f"Student login successful: {request.erp_number}")
        
        return LoginResponse(
            success=True,
            message="Login successful",
            student=student_data
        )
        
    except HTTPException as e:
        logger.warning(f"Login failed for ERP {request.erp_number}: {e.detail}")
        raise e
    except Exception as e:
        logger.error(f"Login error for ERP {request.erp_number}: {str(e)}")
        raise HTTPException(
            status_code=500, 
            detail="Internal server error during login"
        )

@app.post("/chat", response_model=ChatResponse, tags=["Chat"])
def chat(req: ChatRequest):
    """Main chatbot interaction endpoint."""
    global query_count
    try:
        # Optionally inject context
        query = req.query
        if req.year or req.semester or req.course:
            context_str = ""
            if req.year:
                context_str += f"Year: {req.year}. "
            if req.semester:
                context_str += f"Semester: {req.semester}. "
            if req.course:
                context_str += f"Course: {req.course}. "
            query = context_str + query
        response = chatbot.answer_query(query)
        query_count += 1
        return response
    except Exception as e:
        logger.error(f"/chat error: {e}")
        raise HTTPException(status_code=500, detail="Internal server error.")

@app.post("/upload", response_model=UploadResponse, tags=["Documents"])
def upload(files: List[UploadFile] = File(...)):
    """Upload one or more PDF documents for ingestion."""
    save_dir = "uploaded_pdfs"
    os.makedirs(save_dir, exist_ok=True)
    pdf_paths = []
    for file in files:
        if not file.filename or not file.filename.lower().endswith(".pdf"):
            raise HTTPException(status_code=400, detail=f"File {file.filename} is not a PDF.")
        file_path = os.path.join(save_dir, file.filename)
        with open(file_path, "wb") as f:
            f.write(file.file.read())
        pdf_paths.append(file_path)
    try:
        num_chunks = chatbot.process_pdfs(pdf_paths)
        return {"message": f"Processed {len(pdf_paths)} PDFs.", "num_chunks": num_chunks}
    except Exception as e:
        logger.error(f"/upload error: {e}")
        raise HTTPException(status_code=500, detail="Failed to process uploaded PDFs.")

@app.get("/download/{filename}", tags=["Documents"])
def download(filename: str):
    """Download a PDF file by filename."""
    file_path = doc_processor.get_pdf_file(filename)
    if not file_path or not os.path.exists(file_path):
        logger.error(f"File not found for download: {filename}")
        raise HTTPException(status_code=404, detail="File not found.")
    return FileResponse(file_path, media_type="application/pdf", filename=filename)

@app.get("/stats", response_model=StatsResponse, tags=["Utility"])
def stats():
    """Get system statistics."""
    elapsed = time.time() - start_time
    return {"queries_answered": query_count, "uptime_seconds": elapsed}

# --- Error Handling ---
@app.exception_handler(Exception)
def global_exception_handler(request: Request, exc: Exception):
    logger.error(f"Unhandled error: {exc}")
    return JSONResponse(status_code=500, content={"detail": "Internal server error."})
