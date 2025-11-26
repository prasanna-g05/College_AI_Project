# auth.py
# Purpose: FastAPI router for /auth/register (create user) and /auth/login (verify/fetch user).
# Uses models.py (UserCreate/UserLogin/UserResponse) and database.py (get_db session).
# Handles hashing/verification with bcrypt; raises errors for invalid/duplicate data.
# Modular: Included in api_server.py as app.include_router(auth_router, prefix="/auth").
# Security: Never exposes passwords; validates uniqueness (ERP/roll_number).

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer, OAuth2PasswordRequestForm
from pydantic import BaseModel
from sqlalchemy.orm import Session
from typing import Optional
from passlib.context import CryptContext
from jose import JWTError, jwt  # Fixed: Added missing import for 'jwt' (python-jose)
from datetime import datetime, timedelta, timezone
from models import User  # Only SQLAlchemy User from models.py (no Pydantic—defined locally)
from database import get_db
from models import UserResponse

# Create router (groups auth endpoints)
auth_router = APIRouter()

# Pydantic models for requests (keep local—don't import from models.py to avoid conflict)
class UserCreate(BaseModel):
    erp_number: str
    name: str
    year: str
    semester: str
    password: str
    branch: Optional[str] = "Information Technology"
    section: Optional[str] = "A"
    roll_number: Optional[str] = None  # Auto-generate if needed

class UserLogin(BaseModel):
    erp_number: str
    password: str


class TokenWithStudent(BaseModel):
    access_token: str
    token_type: str
    student: Optional[UserResponse]

# Security (unchanged)
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="auth/login")  # For future protected routes
SECRET_KEY = "your-secret-key-change-in-prod"  # From env in prod
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 60

def verify_password(plain_password: str, hashed_password: str) -> bool:
    truncated = plain_password[:72]
    return pwd_context.verify(truncated, hashed_password)

def get_password_hash(password: str) -> str:
    truncated = password[:72]
    return pwd_context.hash(truncated)


def create_access_token(data: dict, expires_delta: Optional[timedelta] = None):
    to_encode = data.copy()
    if expires_delta:
        expire = datetime.now(timezone.utc) + expires_delta
    else:
        expire = datetime.now(timezone.utc) + timedelta(minutes=15)
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)  # Now 'jwt' is defined
    return encoded_jwt

@auth_router.post("/register")
def register_user(user_data: UserCreate, db: Session = Depends(get_db)):
    # Check existing ERP
    existing_erp = db.query(User).filter(User.erp_number == user_data.erp_number).first()
    if existing_erp:
        raise HTTPException(status_code=400, detail="ERP number already registered.")
    
    # Create user (auto-generate roll if missing)
    roll_number = user_data.roll_number or f"{user_data.erp_number[:2]}IT{user_data.erp_number[-3:]}"
    hashed_password = get_password_hash(user_data.password)
    db_user = User(
        erp_number=user_data.erp_number,
        name=user_data.name,
        branch=user_data.branch,
        year=user_data.year,
        semester=user_data.semester,
        section=user_data.section,
        roll_number=roll_number,
        password_hash=hashed_password
    )
    db.add(db_user)
    db.commit()
    db.refresh(db_user)
    return {"message": "User registered successfully", "user": {"erp_number": db_user.erp_number, "name": db_user.name}}

@auth_router.post("/login", response_model=TokenWithStudent)
def login_user(login_data: UserLogin, db: Session = Depends(get_db)):
    """
    Login an existing user by ERP and password.
    - Queries DB by ERP (primary key).
    - Verifies password hash.
    - Returns profile if valid; 401 on fail or inactive.
    - No token yet—add JWT later for sessions.
    """
    # Fetch user by ERP
    db_user = db.query(User).filter(User.erp_number == login_data.erp_number).first()

    # Check if user exists
    if not db_user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid ERP number.",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    # Check if password is correct
    if not verify_password(login_data.password, db_user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid password.",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    # Check if user is active
    if not db_user.is_active:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User account is inactive."
        )
    
    
    # Fixed: Removed redundant db_user.verify_password (doesn't exist in User model)
    # Single verification above is sufficient
    
    access_token_expires = timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    access_token = create_access_token(
        data={"sub": db_user.erp_number}, expires_delta=access_token_expires
    )
    return {
    "access_token": access_token,
    "token_type": "bearer",
    "student": UserResponse.model_validate(db_user)
    }

