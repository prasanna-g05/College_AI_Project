import os

# create_db.py (Updated to match new database.py)
from database import create_tables, engine  # Calls the fixed function
from models import User  # Ensure loaded

create_tables()  # This now imports models internally

# Test query
from database import get_db
db = next(get_db())  # Get session
try:
    result = db.query(User).first()
    print("✅ Query test: Table exists (empty OK).")
    print(f"DB file size after: {os.path.getsize('pce_it.db')} bytes")
except Exception as e:
    print(f"❌ Still failing: {e}")
finally:
    db.close()
