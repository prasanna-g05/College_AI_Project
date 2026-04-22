from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session
from typing import Optional
from passlib.context import CryptContext
from jose import JWTError, jwt
from datetime import datetime, timedelta, timezone
from models import User, ApprovedStudent, UserResponse
from database import get_db


auth_router = APIRouter()


class UserCreate(BaseModel):
    erp_number: str
    name: str
    year: str
    semester: str
    password: str = Field(..., max_length=72)
    branch: Optional[str] = "Information Technology"
    section: Optional[str] = "A"
    roll_number: Optional[str] = None


class UserLogin(BaseModel):
    erp_number: str
    password: str = Field(..., max_length=72)


class TokenWithStudent(BaseModel):
    access_token: str
    token_type: str
    student: Optional[UserResponse]


pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
bearer_scheme = HTTPBearer()


SECRET_KEY = "your-secret-key-change-in-prod"
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
    expire = datetime.now(timezone.utc) + (
        expires_delta if expires_delta else timedelta(minutes=15)
    )
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)


def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(bearer_scheme),
    db: Session = Depends(get_db)
):
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )

    token = credentials.credentials

    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        erp_number: str = payload.get("sub")
        if erp_number is None:
            raise credentials_exception
    except JWTError:
        raise credentials_exception

    user = db.query(User).filter(User.erp_number == erp_number).first()
    if user is None:
        raise credentials_exception

    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User account is inactive"
        )

    return user


def require_admin(current_user: User = Depends(get_current_user)):
    print("DEBUG current user:", current_user.erp_number, current_user.role)
    if current_user.role != "admin":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Admin access required"
        )
    return current_user


@auth_router.post("/register")
def register_user(user_data: UserCreate, db: Session = Depends(get_db)):
    existing_erp = db.query(User).filter(User.erp_number == user_data.erp_number).first()
    if existing_erp:
        raise HTTPException(status_code=400, detail="ERP number already registered.")

    approved_student = db.query(ApprovedStudent).filter(
        ApprovedStudent.erp_number == user_data.erp_number
    ).first()

    if not approved_student:
        raise HTTPException(
            status_code=403,
            detail="Registration not allowed. ERP number is not approved by admin."
        )

    roll_number = user_data.roll_number or f"{user_data.erp_number[:2]}IT{user_data.erp_number[-3:]}"
    hashed_password = get_password_hash(user_data.password)

    db_user = User(
        erp_number=user_data.erp_number,
        name=user_data.name,
        branch=user_data.branch or "Information Technology",
        year=user_data.year,
        semester=user_data.semester,
        section=user_data.section,
        roll_number=roll_number,
        password_hash=hashed_password,
        role="student"
    )

    db.add(db_user)
    db.commit()
    db.refresh(db_user)

    return {
        "message": "User registered successfully",
        "user": {
            "erp_number": db_user.erp_number,
            "name": db_user.name
        }
    }


@auth_router.post("/login", response_model=TokenWithStudent)
def login_user(login_data: UserLogin, db: Session = Depends(get_db)):
    db_user = db.query(User).filter(User.erp_number == login_data.erp_number).first()

    if not db_user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid ERP number.",
        )

    if not verify_password(login_data.password, db_user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid password.",
        )

    if not db_user.is_active:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User account is inactive."
        )

    access_token_expires = timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    access_token = create_access_token(
        data={"sub": db_user.erp_number},
        expires_delta=access_token_expires
    )

    return {
        "access_token": access_token,
        "token_type": "bearer",
        "student": UserResponse.model_validate(db_user)
    }


@auth_router.get("/me", response_model=UserResponse)
def get_me(current_user: User = Depends(get_current_user)):
    return UserResponse.model_validate(current_user)
