from sqlalchemy import Column, String, Boolean
from sqlalchemy.ext.declarative import declarative_base
from pydantic import BaseModel
from typing import Optional
from passlib.context import CryptContext  # For password hashing

# Global DB base (shared across models)
Base = declarative_base()

# Password hashing utility (bcrypt for security)
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


def hash_password(password: str) -> str:
    """Generate a secure bcrypt hash for new users, truncating to 72 chars."""
    truncated = password[:72]
    return pwd_context.hash(truncated)


def verify_password(plain_password: str, hashed_password: str) -> bool:
    """Verify password against stored hash with truncation to 72 chars."""
    truncated = plain_password[:72]
    return pwd_context.verify(truncated, hashed_password)


class User(Base):
    __tablename__ = "users"  # Table name in DB

    erp_number = Column(String(9), primary_key=True, nullable=False)  # 9-digit ERP: Primary key (unique ID)
    name = Column(String(100), nullable=False)  # Student name, required
    branch = Column(String(50), default="Information Technology", nullable=False)  # Fixed for PCE IT
    year = Column(String(20), nullable=False)  # e.g., "1st Year", from admission calc or input
    semester = Column(String(20), nullable=False)  # e.g., "1st Semester"
    section = Column(String(10), nullable=True)  # Optional section (A/B/etc.)
    roll_number = Column(String(20), unique=True, index=True, nullable=True)  # Unique roll_no (separate index)
    password_hash = Column(String(255), nullable=False)  # Hashed password (never store plain)
    role = Column(String(20), default="student", nullable=False)  # "student" or "admin"
    is_active = Column(Boolean, default=True)  # For soft deletes (inactive users)

    def verify_password(self, plain_password: str) -> bool:
        """Check if provided password matches the stored hash with truncation."""
        truncated = plain_password[:72]
        return pwd_context.verify(truncated, self.password_hash)

# Pydantic schemas for API (request/response)—separate from DB for validation/serialization
class UserCreate(BaseModel):  # For /register POST body
    erp_number: str  # 9 digits (primary key)
    name: str
    year: str  # e.g., "1st Year"
    semester: str  # e.g., "1st Semester"
    section: Optional[str] = None
    roll_number: Optional[str] = None
    password: str  # Plain password (hashed on save)
    role: Optional[str] = "student"  # Default

    class Config:
        from_attributes = True  # Allow conversion from SQLAlchemy objects


class UserLogin(BaseModel):  # For /login POST body
    erp_number: str
    password: str  # Required for secure login


class UserResponse(BaseModel):  # For /login GET response (hide sensitive fields)
    erp_number: str  # Primary key
    name: str
    branch: str
    year: str
    semester: str
    section: Optional[str]
    roll_number: Optional[str]
    role: str
    is_active: bool

    model_config = {
        'from_attributes': True
    }
