from sqlalchemy import Column, String, Boolean, Integer, Date, Text, ForeignKey, DateTime
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import relationship
from pydantic import BaseModel
from typing import Optional, List
from passlib.context import CryptContext
from datetime import datetime


Base = declarative_base()


pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


def hash_password(password: str) -> str:
    truncated = password[:72]
    return pwd_context.hash(truncated)


def verify_password(plain_password: str, hashed_password: str) -> bool:
    truncated = plain_password[:72]
    return pwd_context.verify(truncated, hashed_password)


class User(Base):
    __tablename__ = "users"

    erp_number = Column(String(9), primary_key=True, nullable=False)
    name = Column(String(100), nullable=False)
    branch = Column(String(50), default="Information Technology", nullable=False)
    year = Column(String(20), nullable=False)
    semester = Column(String(20), nullable=False)
    section = Column(String(10), nullable=True)
    roll_number = Column(String(20), unique=True, index=True, nullable=True)
    password_hash = Column(String(255), nullable=False)
    role = Column(String(20), default="student", nullable=False)
    is_active = Column(Boolean, default=True)

    def verify_password(self, plain_password: str) -> bool:
        truncated = plain_password[:72]
        return pwd_context.verify(truncated, self.password_hash)


class Company(Base):
    __tablename__ = "companies"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(100), nullable=False, unique=True)
    folder_slug = Column(String(150), nullable=False, unique=True, index=True)
    campus_date = Column(Date, nullable=True)
    eligibility = Column(String(200), nullable=True)
    news = Column(Text, nullable=True)

    resources = relationship("TnpResource", back_populates="company", cascade="all, delete-orphan")
    mock_tests = relationship("MockTest", back_populates="company", cascade="all, delete-orphan")


class TnpResource(Base):
    __tablename__ = "tnp_resources"

    id = Column(Integer, primary_key=True, index=True)
    company_id = Column(Integer, ForeignKey("companies.id"), nullable=False, index=True)
    resource_type = Column(String(20), nullable=False)  # notes or pyq
    title = Column(String(255), nullable=False)
    original_filename = Column(String(255), nullable=False)
    stored_filename = Column(String(255), nullable=False)
    file_path = Column(String(500), nullable=False)
    mime_type = Column(String(100), nullable=True)
    uploaded_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    uploaded_by = Column(String(9), nullable=True)

    company = relationship("Company", back_populates="resources")


class MockTest(Base):
    __tablename__ = "mock_tests"

    id = Column(Integer, primary_key=True, index=True)
    company_id = Column(Integer, ForeignKey("companies.id"), nullable=False, index=True)
    title = Column(String(255), nullable=False)
    description = Column(Text, nullable=True)
    duration_minutes = Column(Integer, nullable=False, default=30)
    total_questions = Column(Integer, default=0, nullable=False)
    total_marks = Column(Integer, default=0, nullable=False)
    uploaded_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    uploaded_by = Column(String(9), nullable=True)
    is_active = Column(Boolean, default=True, nullable=False)

    company = relationship("Company", back_populates="mock_tests")
    questions = relationship("MockQuestion", back_populates="mock_test", cascade="all, delete-orphan")


class MockQuestion(Base):
    __tablename__ = "mock_questions"

    id = Column(Integer, primary_key=True, index=True)
    mock_test_id = Column(Integer, ForeignKey("mock_tests.id"), nullable=False, index=True)
    question_text = Column(Text, nullable=False)
    option_a = Column(String(500), nullable=False)
    option_b = Column(String(500), nullable=False)
    option_c = Column(String(500), nullable=False)
    option_d = Column(String(500), nullable=False)
    correct_option = Column(String(1), nullable=False)  # A, B, C, D
    marks = Column(Integer, default=1, nullable=False)
    question_order = Column(Integer, nullable=False)

    mock_test = relationship("MockTest", back_populates="questions")


class StudentSheet(Base):
    __tablename__ = "student_sheets"

    id = Column(Integer, primary_key=True, index=True)
    file_name = Column(String(255), nullable=False)
    file_path = Column(String(500), nullable=False)
    uploaded_at = Column(DateTime, default=datetime.utcnow)
    approved_count = Column(Integer, default=0)


class ApprovedStudent(Base):
    __tablename__ = "approved_students"

    id = Column(Integer, primary_key=True, index=True)
    erp_number = Column(String(20), unique=True, index=True, nullable=False)
    name = Column(String(100), nullable=False)
    branch = Column(String(50), nullable=True)
    year = Column(String(20), nullable=True)
    semester = Column(String(20), nullable=True)
    section = Column(String(10), nullable=True)
    roll_number = Column(String(20), nullable=True)
    sheet_id = Column(Integer, ForeignKey("student_sheets.id"), nullable=True)


class ChatbotFile(Base):
    __tablename__ = "chatbot_files"

    id = Column(Integer, primary_key=True, index=True)
    original_filename = Column(String(255), nullable=False)
    stored_filename = Column(String(255), unique=True, nullable=False)
    file_path = Column(String(500), nullable=False)
    mime_type = Column(String(100), nullable=True)
    uploaded_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    uploaded_by = Column(String(9), nullable=True)
    is_processed = Column(Boolean, default=False, nullable=False)


class UserCreate(BaseModel):
    erp_number: str
    name: str
    year: str
    semester: str
    section: Optional[str] = None
    roll_number: Optional[str] = None
    password: str
    role: Optional[str] = "student"

    class Config:
        from_attributes = True


class UserLogin(BaseModel):
    erp_number: str
    password: str


class UserResponse(BaseModel):
    erp_number: str
    name: str
    branch: str
    year: str
    semester: str
    section: Optional[str]
    roll_number: Optional[str]
    role: str
    is_active: bool

    model_config = {
        "from_attributes": True
    }


class CompanyCreate(BaseModel):
    name: str
    campus_date: Optional[str] = None
    eligibility: Optional[str] = None
    news: Optional[str] = None


class CompanyResponse(BaseModel):
    id: int
    name: str
    folder_slug: str
    campus_date: Optional[str]
    eligibility: Optional[str]
    news: Optional[str]

    model_config = {
        "from_attributes": True
    }


class StudentSheetResponse(BaseModel):
    id: int
    file_name: str
    file_path: str
    uploaded_at: datetime
    approved_count: int

    model_config = {
        "from_attributes": True
    }


class ApprovedStudentResponse(BaseModel):
    id: int
    erp_number: str
    name: str
    branch: Optional[str] = None
    year: Optional[str] = None
    semester: Optional[str] = None
    section: Optional[str] = None
    roll_number: Optional[str] = None
    sheet_id: Optional[int] = None

    model_config = {
        "from_attributes": True
    }


class ChatbotFileResponse(BaseModel):
    id: int
    original_filename: str
    stored_filename: str
    file_path: str
    mime_type: Optional[str] = None
    uploaded_at: datetime
    uploaded_by: Optional[str] = None
    is_processed: bool

    model_config = {
        "from_attributes": True
    }


class TnpResourceResponse(BaseModel):
    id: int
    company_id: int
    resource_type: str
    title: str
    original_filename: str
    stored_filename: str
    file_path: str
    mime_type: Optional[str] = None
    uploaded_at: datetime
    uploaded_by: Optional[str] = None

    model_config = {
        "from_attributes": True
    }


class MockQuestionCreate(BaseModel):
    question_text: str
    option_a: str
    option_b: str
    option_c: str
    option_d: str
    correct_option: str
    marks: Optional[int] = 1
    question_order: int


class MockTestCreate(BaseModel):
    title: str
    description: Optional[str] = None
    duration_minutes: int
    questions: List[MockQuestionCreate]


class MockQuestionResponse(BaseModel):
    id: int
    question_text: str
    option_a: str
    option_b: str
    option_c: str
    option_d: str
    question_order: int
    marks: int

    model_config = {
        "from_attributes": True
    }


class MockTestResponse(BaseModel):
    id: int
    company_id: int
    title: str
    description: Optional[str] = None
    duration_minutes: int
    total_questions: int
    total_marks: int
    uploaded_at: datetime
    uploaded_by: Optional[str] = None
    is_active: bool

    model_config = {
        "from_attributes": True
    }


class MockTestDetailResponse(BaseModel):
    id: int
    company_id: int
    title: str
    description: Optional[str] = None
    duration_minutes: int
    total_questions: int
    total_marks: int
    questions: List[MockQuestionResponse]

    model_config = {
        "from_attributes": True
    }


class MockAnswerItem(BaseModel):
    question_id: int
    selected_option: str


class MockTestSubmitRequest(BaseModel):
    answers: List[MockAnswerItem]


class MockTestSubmitResponse(BaseModel):
    mock_test_id: int
    total_questions: int
    attempted_questions: int
    correct_answers: int
    wrong_answers: int
    score: int
    total_marks: int
