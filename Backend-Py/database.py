import os
# database.py
# Purpose: Engine, session, create_tables()—now imports models inside create_tables() for reliable schema creation.

import sqlalchemy  # For inspector
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker
from sqlalchemy.exc import OperationalError

# Absolute path for reliability (adjust if project not in D:\Project)
DB_PATH = r"D:\Project\pce_it.db"
SQLALCHEMY_DATABASE_URL = f"sqlite:///{DB_PATH}"
engine = create_engine(
    SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False}  # SQLite multi-thread fix
)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

def create_tables():
    """Creates tables—imports models here to ensure metadata populated."""
    # Import models to register User (and any future models) with Base
    from models import Base, User  # Triggers __tablename__ registration
    
    try:
        # Log before
        inspector = sqlalchemy.inspect(engine)
        # Before (file size only—inspector unreliable)
        file_size_before = os.path.getsize(DB_PATH) if os.path.exists(DB_PATH) else 0
        print(f"DB file before creation: {file_size_before} bytes")
        
        Base.metadata.create_all(bind=engine)  # Runs (populates metadata)
        
        # After: Raw SQL check
        file_size_after = os.path.getsize(DB_PATH) if os.path.exists(DB_PATH) else 0
        with engine.connect() as conn:
            result = conn.execute(text("SELECT name FROM sqlite_master WHERE type='table';"))
            raw_tables = [row[0] for row in result.fetchall()]
            print(f"Raw SQL tables after: {raw_tables} (file size: {file_size_after} bytes)")
            
            # Column check for 'users'
            if 'users' in raw_tables:
                col_result = conn.execute(text("PRAGMA table_info(users);"))
                columns = [row[1] for row in col_result.fetchall()]
                print(f"✅ 'users' confirmed with columns: {columns[:5]}... (file >0 bytes).")
            else:
                print("❌ Raw SQL: No 'users'—check models.py.")
                
    except OperationalError as e:
        print(f"❌ DB OperationalError: {e}. Delete pce_it.db and retry.")
    except Exception as e:
        print(f"❌ Unexpected DB error: {e}. Ensure models.py exists with User class.")

# Add this import at top for file size check (standard lib)

