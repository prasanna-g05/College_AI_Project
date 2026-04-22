import os
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker
from sqlalchemy.exc import OperationalError

# Use env var if available, otherwise default to local file
DB_PATH = os.getenv("DB_PATH", "pce_it.db")
SQLALCHEMY_DATABASE_URL = f"sqlite:///{DB_PATH}"

engine = create_engine(
    SQLALCHEMY_DATABASE_URL,
    connect_args={"check_same_thread": False}
)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def create_tables():
    """Create all DB tables after importing models so metadata is populated."""
    from models import (
        Base,
        User,
        Company,
        StudentSheet,
        ApprovedStudent,
        ChatbotFile,
        TnpResource,
        MockTest,
        MockQuestion,
    )

    try:
        file_size_before = os.path.getsize(DB_PATH) if os.path.exists(DB_PATH) else 0
        print(f"DB file before creation: {file_size_before} bytes")

        Base.metadata.create_all(bind=engine)

        file_size_after = os.path.getsize(DB_PATH) if os.path.exists(DB_PATH) else 0

        with engine.connect() as conn:
            result = conn.execute(text("SELECT name FROM sqlite_master WHERE type='table';"))
            raw_tables = [row[0] for row in result.fetchall()]
            print(f"Raw SQL tables after: {raw_tables} (file size: {file_size_after} bytes)")

            expected_tables = [
                "users",
                "companies",
                "student_sheets",
                "approved_students",
                "chatbot_files",
                "tnp_resources",
                "mock_tests",
                "mock_questions",
            ]

            for table_name in expected_tables:
                if table_name in raw_tables:
                    print(f"✅ Table '{table_name}' exists.")
                else:
                    print(f"❌ Table '{table_name}' is missing.")

    except OperationalError as e:
        print(f"❌ DB OperationalError: {e}. Delete pce_it.db and retry.")
    except Exception as e:
        print(f"❌ Unexpected DB error: {e}. Ensure models.py is correct.")
