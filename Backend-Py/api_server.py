# api_server.py
# Purpose: Main FastAPI app for auth, RAG chatbot, admin users, T&P, student sheets,
# chatbot file management, and T&P enhancement resources/mock tests.

from pydantic import BaseModel
from typing import Optional, List
from fastapi import FastAPI, UploadFile, File, HTTPException, Depends, Form, Request
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session
from datetime import datetime
import os
import shutil
import pandas as pd
import re
from dotenv import load_dotenv

load_dotenv()

from auth import auth_router, require_admin
from database import create_tables, get_db
from models import (
    User,
    Company,
    StudentSheet,
    ApprovedStudent,
    ChatbotFile,
    TnpResource,
    MockTest,
    MockQuestion,
    StudentSheetResponse,
    ApprovedStudentResponse,
    ChatbotFileResponse,
    CompanyCreate,
    CompanyResponse,
    TnpResourceResponse,
    MockTestCreate,
    MockTestResponse,
    MockQuestionResponse,
    MockTestDetailResponse,
    MockTestSubmitRequest,
    MockTestSubmitResponse,
)

try:
    from rag_chatbot import RAGChatbot
    chatbot = RAGChatbot(
        persist_directory="chroma_db",
        embedding_model="all-MiniLM-L6-v2"
    )
    RAG_AVAILABLE = True
    print("✅ RAGChatbot instantiated successfully.")
except ImportError as e:
    print(f"❌ RAG import error: {e}")
    RAG_AVAILABLE = False
except Exception as e:
    print(f"❌ RAG init error: {e}")
    RAG_AVAILABLE = False

app = FastAPI(
    title="PCE IT Assistant API",
    version="2.4",
    description="RAG Chatbot + User Auth + Admin + Student Sheets + TNP Enhancements"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

BASE_UPLOAD_DIR = "uploaded_files"
CHATBOT_FILES_DIR = os.path.join(BASE_UPLOAD_DIR, "chatbot")
STUDENT_SHEETS_DIR = os.path.join(BASE_UPLOAD_DIR, "student_sheets")
TNP_FILES_DIR = os.path.join(BASE_UPLOAD_DIR, "tnp")

os.makedirs(CHATBOT_FILES_DIR, exist_ok=True)
os.makedirs(STUDENT_SHEETS_DIR, exist_ok=True)
os.makedirs(TNP_FILES_DIR, exist_ok=True)

app.mount("/files", StaticFiles(directory=CHATBOT_FILES_DIR), name="files")
app.include_router(auth_router, prefix="/auth", tags=["Auth"])


class AdminUserResponse(BaseModel):
    erp_number: str
    name: str
    role: str
    branch: str
    year: str

    model_config = {"from_attributes": True}


class ToggleAdminRequest(BaseModel):
    role: str


class ToggleRoleResponse(BaseModel):
    message: str
    user: AdminUserResponse


class ChatRequest(BaseModel):
    query: str
    erp_number: Optional[str] = None
    semester: Optional[str] = None
    year: Optional[str] = None


@app.on_event("startup")
def startup_event():
    create_tables()
    print("✅ DB setup complete. RAG status:", "Available" if RAG_AVAILABLE else "Disabled")


def normalize_column_name(col: str) -> str:
    return col.strip().lower().replace(" ", "_")


def get_unique_filename(directory: str, filename: str) -> str:
    base_name, ext = os.path.splitext(filename)
    counter = 1
    candidate = filename

    while os.path.exists(os.path.join(directory, candidate)):
        candidate = f"{base_name}_{counter}{ext}"
        counter += 1

    return candidate


def slugify_company_name(name: str) -> str:
    slug = name.strip().lower()
    slug = re.sub(r"[^a-z0-9]+", "_", slug)
    slug = re.sub(r"_+", "_", slug).strip("_")
    return slug


def ensure_unique_company_slug(db: Session, company_name: str) -> str:
    base_slug = slugify_company_name(company_name)
    slug = base_slug
    counter = 1

    while db.query(Company).filter(Company.folder_slug == slug).first():
        slug = f"{base_slug}_{counter}"
        counter += 1

    return slug


def get_company_base_dir(folder_slug: str) -> str:
    return os.path.join(TNP_FILES_DIR, folder_slug)


def get_company_resource_dir(folder_slug: str, resource_type: str) -> str:
    return os.path.join(TNP_FILES_DIR, folder_slug, resource_type)


def create_company_resource_folders(folder_slug: str):
    os.makedirs(get_company_resource_dir(folder_slug, "notes"), exist_ok=True)
    os.makedirs(get_company_resource_dir(folder_slug, "pyqs"), exist_ok=True)
    os.makedirs(get_company_resource_dir(folder_slug, "mock_tests"), exist_ok=True)


def build_tnp_resource_response(resource: TnpResource, request: Request):
    return {
        "id": resource.id,
        "company_id": resource.company_id,
        "resource_type": resource.resource_type,
        "title": resource.title,
        "original_filename": resource.original_filename,
        "stored_filename": resource.stored_filename,
        "file_path": resource.file_path,
        "mime_type": resource.mime_type,
        "uploaded_at": resource.uploaded_at.isoformat() if resource.uploaded_at else None,
        "uploaded_by": resource.uploaded_by,
        "download_url": str(request.url_for("download_tnp_resource", resource_id=resource.id))
    }


def parse_student_sheet(file_path: str):
    ext = os.path.splitext(file_path)[1].lower()

    if ext == ".csv":
        df = pd.read_csv(file_path)
    elif ext == ".xlsx":
        df = pd.read_excel(file_path)
    else:
        raise ValueError("Only CSV and XLSX files are supported.")

    df.columns = [normalize_column_name(col) for col in df.columns]

    required_columns = {"erp_number", "name"}
    if not required_columns.issubset(set(df.columns)):
        raise ValueError("Sheet must contain at least 'erp_number' and 'name' columns.")

    students = []

    for _, row in df.iterrows():
        erp_number = str(row.get("erp_number", "")).strip()
        name = str(row.get("name", "")).strip()

        if not erp_number or erp_number.lower() == "nan":
            continue
        if not name or name.lower() == "nan":
            continue

        students.append({
            "erp_number": erp_number,
            "name": name,
            "branch": str(row.get("branch", "")).strip() or None,
            "year": str(row.get("year", "")).strip() or None,
            "semester": str(row.get("semester", "")).strip() or None,
            "section": str(row.get("section", "")).strip() or None,
            "roll_number": str(row.get("roll_number", "")).strip() or None,
        })

    return students


def is_pdf_file(filename: str, content_type: Optional[str]) -> bool:
    ext = os.path.splitext(filename)[1].lower()
    return ext == ".pdf" and (content_type in ["application/pdf", "application/octet-stream", None])


def cleanup_pdf_metadata(filename: str):
    try:
        from document_processor import PDF_METADATA_FILE
        import json

        if not os.path.exists(PDF_METADATA_FILE):
            return

        with open(PDF_METADATA_FILE, "r", encoding="utf-8") as f:
            metadata = json.load(f)

        if filename in metadata:
            del metadata[filename]

            with open(PDF_METADATA_FILE, "w", encoding="utf-8") as f:
                json.dump(metadata, f, indent=2, ensure_ascii=False)

    except Exception as e:
        print(f"⚠️ Failed to clean metadata for {filename}: {e}")


def parse_mock_test_file(file_path: str):
    ext = os.path.splitext(file_path)[1].lower()

    if ext == ".csv":
        df = pd.read_csv(file_path)
    elif ext == ".xlsx":
        df = pd.read_excel(file_path)
    else:
        raise ValueError("Only CSV and XLSX files are supported for mock tests.")

    df.columns = [normalize_column_name(col) for col in df.columns]

    required_columns = {
        "question_text", "option_a", "option_b", "option_c", "option_d", "correct_option"
    }
    if not required_columns.issubset(set(df.columns)):
        raise ValueError(
            "Mock test sheet must contain question_text, option_a, option_b, option_c, option_d, correct_option."
        )

    questions = []
    for index, row in df.iterrows():
        correct_option = str(row.get("correct_option", "")).strip().upper()
        if correct_option not in {"A", "B", "C", "D"}:
            raise ValueError(f"Invalid correct_option at row {index + 1}. Must be A/B/C/D.")

        questions.append({
            "question_text": str(row.get("question_text", "")).strip(),
            "option_a": str(row.get("option_a", "")).strip(),
            "option_b": str(row.get("option_b", "")).strip(),
            "option_c": str(row.get("option_c", "")).strip(),
            "option_d": str(row.get("option_d", "")).strip(),
            "correct_option": correct_option,
            "marks": int(row.get("marks", 1)) if str(row.get("marks", "")).strip() else 1,
            "question_order": int(row.get("question_order", index + 1)) if str(row.get("question_order", "")).strip() else index + 1,
        })

    if not questions:
        raise ValueError("No valid questions found in mock test file.")

    return questions


@app.get("/admin/users", response_model=List[AdminUserResponse], tags=["Admin"])
def get_all_users(
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin)
):
    users = db.query(User).all()
    return [AdminUserResponse.model_validate(user) for user in users]


@app.put("/admin/users/{erp_number}/role", response_model=ToggleRoleResponse, tags=["Admin"])
def toggle_user_role(
    erp_number: str,
    request: ToggleAdminRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin)
):
    user = db.query(User).filter(User.erp_number == erp_number).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    if request.role not in ["admin", "student"]:
        raise HTTPException(status_code=400, detail="Role must be 'admin' or 'student'")

    user.role = request.role
    db.commit()
    db.refresh(user)

    return {
        "message": f"User {erp_number} role changed to {request.role}",
        "user": AdminUserResponse.model_validate(user)
    }


@app.post("/admin/companies", response_model=CompanyResponse, tags=["Admin"])
def create_company(
    company: CompanyCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin)
):
    existing_company = db.query(Company).filter(Company.name == company.name).first()
    if existing_company:
        raise HTTPException(status_code=400, detail="Company with this name already exists")

    campus_date = None
    if company.campus_date:
        try:
            campus_date = datetime.strptime(company.campus_date, "%Y-%m-%d").date()
        except ValueError:
            raise HTTPException(
                status_code=400,
                detail="Invalid campus_date format. Use YYYY-MM-DD (e.g., 2026-10-12)"
            )

    folder_slug = ensure_unique_company_slug(db, company.name)

    db_company = Company(
        name=company.name,
        folder_slug=folder_slug,
        campus_date=campus_date,
        eligibility=company.eligibility,
        news=company.news,
    )
    db.add(db_company)
    db.commit()
    db.refresh(db_company)

    create_company_resource_folders(folder_slug)

    return {
        "id": db_company.id,
        "name": db_company.name,
        "folder_slug": db_company.folder_slug,
        "campus_date": db_company.campus_date.isoformat() if db_company.campus_date else None,
        "eligibility": db_company.eligibility,
        "news": db_company.news,
    }


@app.get("/admin/companies", response_model=List[CompanyResponse], tags=["Admin"])
def get_companies(
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin)
):
    companies = db.query(Company).all()
    return [
        {
            "id": company.id,
            "name": company.name,
            "folder_slug": company.folder_slug,
            "campus_date": company.campus_date.isoformat() if company.campus_date else None,
            "eligibility": company.eligibility,
            "news": company.news,
        }
        for company in companies
    ]


@app.get("/tnp/companies", response_model=List[CompanyResponse], tags=["TNP"])
def get_tnp_companies(db: Session = Depends(get_db)):
    companies = db.query(Company).all()
    return [
        {
            "id": company.id,
            "name": company.name,
            "folder_slug": company.folder_slug,
            "campus_date": company.campus_date.isoformat() if company.campus_date else None,
            "eligibility": company.eligibility,
            "news": company.news,
        }
        for company in companies
    ]


@app.get("/tnp/resources/{resource_id}/download", tags=["TNP"], name="download_tnp_resource")
def download_tnp_resource(resource_id: int, db: Session = Depends(get_db)):
    resource = db.query(TnpResource).filter(TnpResource.id == resource_id).first()
    if not resource:
        raise HTTPException(status_code=404, detail="Resource not found")

    if not resource.file_path or not os.path.isfile(resource.file_path):
        raise HTTPException(status_code=404, detail="File not found")

    return FileResponse(
        path=resource.file_path,
        media_type=resource.mime_type or "application/pdf",
        filename=resource.original_filename
    )


@app.get("/tnp/companies/{company_id}", tags=["TNP"])
def get_company_detail(company_id: int, request: Request, db: Session = Depends(get_db)):
    company = db.query(Company).filter(Company.id == company_id).first()
    if not company:
        raise HTTPException(status_code=404, detail="Company not found")

    notes = db.query(TnpResource).filter(
        TnpResource.company_id == company_id,
        TnpResource.resource_type == "notes"
    ).order_by(TnpResource.uploaded_at.desc()).all()

    pyqs = db.query(TnpResource).filter(
        TnpResource.company_id == company_id,
        TnpResource.resource_type == "pyqs"
    ).order_by(TnpResource.uploaded_at.desc()).all()

    mock_tests = db.query(MockTest).filter(
        MockTest.company_id == company_id,
        MockTest.is_active == True
    ).order_by(MockTest.uploaded_at.desc()).all()

    return {
        "company": {
            "id": company.id,
            "name": company.name,
            "folder_slug": company.folder_slug,
            "campus_date": company.campus_date.isoformat() if company.campus_date else None,
            "eligibility": company.eligibility,
            "news": company.news,
        },
        "notes": [build_tnp_resource_response(item, request) for item in notes],
        "pyqs": [build_tnp_resource_response(item, request) for item in pyqs],
        "mock_tests": [MockTestResponse.model_validate(item) for item in mock_tests]
    }


@app.delete("/admin/companies/{company_id}", tags=["Admin"])
def delete_company(
    company_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin)
):
    company = db.query(Company).filter(Company.id == company_id).first()
    if not company:
        raise HTTPException(status_code=404, detail="Company not found")

    company_dir = get_company_base_dir(company.folder_slug)

    try:
        if os.path.exists(company_dir):
            shutil.rmtree(company_dir)

        db.delete(company)
        db.commit()
        return {"message": "Company deleted successfully"}

    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=500, detail=f"Company deletion failed: {str(e)}")


@app.post("/admin/companies/{company_id}/resources/{resource_type}", response_model=TnpResourceResponse, tags=["Admin"])
async def upload_tnp_resource(
    company_id: int,
    resource_type: str,
    title: str = Form(...),
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin)
):
    if resource_type not in ["notes", "pyqs"]:
        raise HTTPException(status_code=400, detail="resource_type must be 'notes' or 'pyqs'")

    company = db.query(Company).filter(Company.id == company_id).first()
    if not company:
        raise HTTPException(status_code=404, detail="Company not found")

    if not file.filename:
        raise HTTPException(status_code=400, detail="Filename is required")

    if not is_pdf_file(file.filename, file.content_type):
        raise HTTPException(status_code=400, detail="Only PDF files are allowed")

    target_dir = get_company_resource_dir(company.folder_slug, resource_type)
    os.makedirs(target_dir, exist_ok=True)

    stored_filename = get_unique_filename(target_dir, file.filename)
    file_path = os.path.join(target_dir, stored_filename)

    try:
        with open(file_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)

        db_resource = TnpResource(
            company_id=company.id,
            resource_type=resource_type,
            title=title,
            original_filename=file.filename,
            stored_filename=stored_filename,
            file_path=file_path,
            mime_type=file.content_type or "application/pdf",
            uploaded_by=current_user.erp_number
        )
        db.add(db_resource)
        db.commit()
        db.refresh(db_resource)

        return db_resource

    except Exception as e:
        db.rollback()
        if os.path.exists(file_path):
            os.remove(file_path)
        raise HTTPException(status_code=500, detail=f"Resource upload failed: {str(e)}")


@app.get("/admin/companies/{company_id}/resources", response_model=List[TnpResourceResponse], tags=["Admin"])
def get_company_resources_admin(
    company_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin)
):
    company = db.query(Company).filter(Company.id == company_id).first()
    if not company:
        raise HTTPException(status_code=404, detail="Company not found")

    return db.query(TnpResource).filter(
        TnpResource.company_id == company_id
    ).order_by(TnpResource.uploaded_at.desc()).all()


@app.delete("/admin/resources/{resource_id}", tags=["Admin"])
def delete_tnp_resource(
    resource_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin)
):
    resource = db.query(TnpResource).filter(TnpResource.id == resource_id).first()
    if not resource:
        raise HTTPException(status_code=404, detail="Resource not found")

    try:
        if resource.file_path and os.path.exists(resource.file_path):
            os.remove(resource.file_path)

        db.delete(resource)
        db.commit()
        return {"message": "Resource deleted successfully"}

    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=500, detail=f"Resource deletion failed: {str(e)}")


@app.post("/admin/companies/{company_id}/mock-tests/upload", response_model=MockTestResponse, tags=["Admin"])
async def upload_mock_test(
    company_id: int,
    title: str = Form(...),
    description: str = Form(""),
    duration_minutes: int = Form(...),
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin)
):
    company = db.query(Company).filter(Company.id == company_id).first()
    if not company:
        raise HTTPException(status_code=404, detail="Company not found")

    if not file.filename:
        raise HTTPException(status_code=400, detail="Filename is required")

    ext = os.path.splitext(file.filename)[1].lower()
    if ext not in [".csv", ".xlsx"]:
        raise HTTPException(status_code=400, detail="Only CSV and XLSX files are allowed for mock tests")

    mock_dir = get_company_resource_dir(company.folder_slug, "mock_tests")
    os.makedirs(mock_dir, exist_ok=True)

    stored_filename = get_unique_filename(mock_dir, file.filename)
    file_path = os.path.join(mock_dir, stored_filename)

    try:
        with open(file_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)

        questions_data = parse_mock_test_file(file_path)
        total_marks = sum(item["marks"] for item in questions_data)

        db_mock_test = MockTest(
            company_id=company.id,
            title=title,
            description=description or None,
            duration_minutes=duration_minutes,
            total_questions=len(questions_data),
            total_marks=total_marks,
            uploaded_by=current_user.erp_number,
            is_active=True
        )
        db.add(db_mock_test)
        db.commit()
        db.refresh(db_mock_test)

        for item in questions_data:
            db_question = MockQuestion(
                mock_test_id=db_mock_test.id,
                question_text=item["question_text"],
                option_a=item["option_a"],
                option_b=item["option_b"],
                option_c=item["option_c"],
                option_d=item["option_d"],
                correct_option=item["correct_option"],
                marks=item["marks"],
                question_order=item["question_order"]
            )
            db.add(db_question)

        db.commit()
        db.refresh(db_mock_test)

        return db_mock_test

    except Exception as e:
        db.rollback()
        if os.path.exists(file_path):
            os.remove(file_path)
        raise HTTPException(status_code=500, detail=f"Mock test upload failed: {str(e)}")


@app.get("/admin/companies/{company_id}/mock-tests", response_model=List[MockTestResponse], tags=["Admin"])
def get_mock_tests_admin(
    company_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin)
):
    company = db.query(Company).filter(Company.id == company_id).first()
    if not company:
        raise HTTPException(status_code=404, detail="Company not found")

    return db.query(MockTest).filter(
        MockTest.company_id == company_id
    ).order_by(MockTest.uploaded_at.desc()).all()


@app.get("/tnp/mock-tests/{mock_test_id}", response_model=MockTestDetailResponse, tags=["TNP"])
def get_mock_test_for_student(mock_test_id: int, db: Session = Depends(get_db)):
    mock_test = db.query(MockTest).filter(
        MockTest.id == mock_test_id,
        MockTest.is_active == True
    ).first()
    if not mock_test:
        raise HTTPException(status_code=404, detail="Mock test not found")

    questions = db.query(MockQuestion).filter(
        MockQuestion.mock_test_id == mock_test.id
    ).order_by(MockQuestion.question_order.asc()).all()

    return {
        "id": mock_test.id,
        "company_id": mock_test.company_id,
        "title": mock_test.title,
        "description": mock_test.description,
        "duration_minutes": mock_test.duration_minutes,
        "total_questions": mock_test.total_questions,
        "total_marks": mock_test.total_marks,
        "questions": [MockQuestionResponse.model_validate(q) for q in questions]
    }


@app.post("/tnp/mock-tests/{mock_test_id}/submit", response_model=MockTestSubmitResponse, tags=["TNP"])
def submit_mock_test(
    mock_test_id: int,
    payload: MockTestSubmitRequest,
    db: Session = Depends(get_db)
):
    mock_test = db.query(MockTest).filter(
        MockTest.id == mock_test_id,
        MockTest.is_active == True
    ).first()
    if not mock_test:
        raise HTTPException(status_code=404, detail="Mock test not found")

    questions = db.query(MockQuestion).filter(
        MockQuestion.mock_test_id == mock_test.id
    ).all()

    question_map = {q.id: q for q in questions}
    attempted_questions = 0
    correct_answers = 0
    wrong_answers = 0
    score = 0

    for answer in payload.answers:
        question = question_map.get(answer.question_id)
        if not question:
            continue

        selected = answer.selected_option.strip().upper()
        if selected not in {"A", "B", "C", "D"}:
            continue

        attempted_questions += 1

        if selected == question.correct_option:
            correct_answers += 1
            score += question.marks
        else:
            wrong_answers += 1

    return {
        "mock_test_id": mock_test.id,
        "total_questions": mock_test.total_questions,
        "attempted_questions": attempted_questions,
        "correct_answers": correct_answers,
        "wrong_answers": wrong_answers,
        "score": score,
        "total_marks": mock_test.total_marks
    }


@app.post("/admin/student-sheets/upload", tags=["Admin"])
async def upload_student_sheet(
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin)
):
    ext = os.path.splitext(file.filename)[1].lower()
    if ext not in [".csv", ".xlsx"]:
        raise HTTPException(status_code=400, detail="Only CSV and XLSX files are allowed.")

    stored_filename = get_unique_filename(STUDENT_SHEETS_DIR, file.filename)
    file_path = os.path.join(STUDENT_SHEETS_DIR, stored_filename)

    with open(file_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    try:
        students = parse_student_sheet(file_path)

        sheet_log = StudentSheet(
            file_name=stored_filename,
            file_path=file_path,
            approved_count=0
        )
        db.add(sheet_log)
        db.commit()
        db.refresh(sheet_log)

        inserted_count = 0
        duplicate_count = 0
        existing_erps = set(row[0] for row in db.query(ApprovedStudent.erp_number).all())

        for student_data in students:
            erp_number = student_data["erp_number"]

            if erp_number in existing_erps:
                duplicate_count += 1
                continue

            approved_student = ApprovedStudent(
                erp_number=erp_number,
                name=student_data["name"],
                branch=student_data["branch"],
                year=student_data["year"],
                semester=student_data["semester"],
                section=student_data["section"],
                roll_number=student_data["roll_number"],
                sheet_id=sheet_log.id
            )
            db.add(approved_student)
            inserted_count += 1
            existing_erps.add(erp_number)

        sheet_log.approved_count = inserted_count
        db.commit()

        return {
            "message": "Student sheet uploaded successfully.",
            "sheet_id": sheet_log.id,
            "file_name": stored_filename,
            "new_students": inserted_count,
            "duplicates_skipped": duplicate_count,
            "total_processed": len(students)
        }

    except Exception as e:
        db.rollback()
        if os.path.exists(file_path):
            os.remove(file_path)
        raise HTTPException(status_code=500, detail=f"Student sheet processing failed: {str(e)}")


@app.get("/admin/student-sheets", response_model=List[StudentSheetResponse], tags=["Admin"])
def get_student_sheets(
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin)
):
    return db.query(StudentSheet).order_by(StudentSheet.uploaded_at.desc()).all()


@app.get("/admin/approved-students", response_model=List[ApprovedStudentResponse], tags=["Admin"])
def get_approved_students(
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin)
):
    return db.query(ApprovedStudent).order_by(ApprovedStudent.name.asc()).all()


@app.post("/admin/chatbot-files/upload", response_model=ChatbotFileResponse, tags=["Admin"])
async def upload_chatbot_file(
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin)
):
    if not file.filename:
        raise HTTPException(status_code=400, detail="Filename is required.")

    if not is_pdf_file(file.filename, file.content_type):
        raise HTTPException(status_code=400, detail="Only PDF files are allowed.")

    if not RAG_AVAILABLE:
        raise HTTPException(status_code=503, detail="RAG service unavailable.")

    stored_filename = get_unique_filename(CHATBOT_FILES_DIR, file.filename)
    file_path = os.path.join(CHATBOT_FILES_DIR, stored_filename)

    try:
        with open(file_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)

        db_file = ChatbotFile(
            original_filename=file.filename,
            stored_filename=stored_filename,
            file_path=file_path,
            mime_type=file.content_type or "application/pdf",
            uploaded_by=current_user.erp_number,
            is_processed=False
        )
        db.add(db_file)
        db.commit()
        db.refresh(db_file)

        chatbot.process_pdfs([file_path])

        db_file.is_processed = True
        db.commit()
        db.refresh(db_file)

        return db_file

    except Exception as e:
        db.rollback()
        if os.path.exists(file_path):
            os.remove(file_path)

        existing_file = db.query(ChatbotFile).filter(
            ChatbotFile.stored_filename == stored_filename
        ).first()
        if existing_file:
            db.delete(existing_file)
            db.commit()

        raise HTTPException(status_code=500, detail=f"Chatbot file upload failed: {str(e)}")


@app.get("/admin/chatbot-files", response_model=List[ChatbotFileResponse], tags=["Admin"])
def get_chatbot_files(
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin)
):
    return db.query(ChatbotFile).order_by(ChatbotFile.uploaded_at.desc()).all()


@app.delete("/admin/chatbot-files/{file_id}", tags=["Admin"])
def delete_chatbot_file(
    file_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin)
):
    db_file = db.query(ChatbotFile).filter(ChatbotFile.id == file_id).first()
    if not db_file:
        raise HTTPException(status_code=404, detail="Chatbot file not found")

    try:
        if db_file.file_path and os.path.exists(db_file.file_path):
            os.remove(db_file.file_path)

        chatbot.doc_processor.delete_pdf_metadata_entry(db_file.stored_filename)

        db.delete(db_file)
        db.commit()

        return {"message": "Chatbot file deleted successfully"}

    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=500, detail=f"Chatbot file deletion failed: {str(e)}")


@app.post("/upload", tags=["RAG"])
async def upload_files(files: List[UploadFile] = File(...)):
    if not files:
        raise HTTPException(status_code=400, detail="No files provided.")
    if not RAG_AVAILABLE:
        raise HTTPException(status_code=503, detail="RAG service unavailable.")

    pdf_paths = []

    try:
        for file in files:
            if not file.filename or not is_pdf_file(file.filename, file.content_type):
                raise HTTPException(status_code=400, detail="Only PDFs allowed.")

            stored_filename = get_unique_filename(CHATBOT_FILES_DIR, file.filename)
            file_path = os.path.join(CHATBOT_FILES_DIR, stored_filename)

            with open(file_path, "wb") as buffer:
                shutil.copyfileobj(file.file, buffer)

            pdf_paths.append(file_path)

        result = chatbot.process_pdfs(pdf_paths)

        return {
            "message": f"{len(pdf_paths)} PDFs uploaded and processed to Chroma.",
            "result": result
        }

    except Exception as e:
        for path in pdf_paths:
            if os.path.exists(path):
                os.remove(path)
        raise HTTPException(status_code=500, detail=f"Processing failed: {str(e)}")


@app.post("/chat", tags=["RAG"])
async def chat_endpoint(request: ChatRequest):
    if not request.query:
        raise HTTPException(status_code=400, detail="Query required.")
    if not RAG_AVAILABLE:
        raise HTTPException(status_code=503, detail="RAG service unavailable.")

    response = chatbot.answer_query(request.query)

    if response.get("type") == "file_selection":
        return {
            "type": "file_selection",
            "message": response["answer"],
            "matching_files": [
                {
                    "filename": item["filename"],
                    "download_url": item.get("download_url", f"/download/{item['filename']}")
                }
                for item in response["matching_files"]
            ]
        }

    if request.erp_number and "answer" in response:
        response["answer"] += f" (Personalized for ERP: {request.erp_number})"

    return response


@app.get("/download/{filename}", tags=["RAG"])
async def download_file(filename: str):
    safe_filename = os.path.basename(filename)
    file_path = os.path.join(CHATBOT_FILES_DIR, safe_filename)

    if os.path.exists(file_path) and os.path.isfile(file_path):
        return FileResponse(file_path, media_type="application/pdf", filename=safe_filename)

    raise HTTPException(status_code=404, detail="File not found.")


@app.get("/")
def root():
    return {
        "message": "PCE IT Assistant API v2.4 - RAG + Auth + Admin + Student Sheets + TNP Enhancements Ready. /docs for testing."
    }


@app.get("/health")
def health_check():
    return {
        "status": "healthy",
        "rag_available": RAG_AVAILABLE,
        "version": "2.4"
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
