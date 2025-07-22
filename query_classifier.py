from typing import Literal, Dict
import re

PDF_KEYWORDS = [
    "syllabus", "timetable", "faculty", "faculty details", "course content", 
    "semester subjects", "HOD", "department", "project", "workflow", "student portal"
]

# UPDATED - More comprehensive content explanation keywords
GENERAL_EDU_KEYWORDS = [
    "what is", "explain", "difference between", "how to", "benefits of", 
    "define", "describe", "purpose of", "tell me about", "information about",
    "meaning of", "definition of", "how does", "why does", "overview of",
    "summary of", "details about", "features of", "advantages of"
]

NON_EDU_KEYWORDS = [
    "weather", "movie", "joke", "news", "sports", "game", "music", "song", 
    "restaurant", "hotel", "politics", "celebrities"
]

QueryType = Literal["pdf", "general", "non-educational"]

def classify_query(query: str) -> QueryType:
    """
    UPDATED - Classifies the query as 'pdf', 'general', or 'non-educational'.
    Better handling of content explanation vs file download requests.
    """
    q = query.lower()
    
    # UPDATED - First check for explanation/content requests
    has_content_intent = any(k in q for k in GENERAL_EDU_KEYWORDS)
    has_pdf_keywords = any(k in q for k in PDF_KEYWORDS)
    
    # If it has both explanation words AND PDF keywords, treat as PDF content query
    if has_content_intent and has_pdf_keywords:
        return "pdf"  # This will retrieve PDF content and provide text answer
    
    # If it only has explanation words, treat as general educational
    if has_content_intent:
        return "general"
    
    # Check for PDF-related queries without explanation intent
    if has_pdf_keywords:
        return "pdf"
    
    # Check for non-educational content
    if any(k in q for k in NON_EDU_KEYWORDS):
        return "non-educational"
    
    # Default: treat as general educational
    return "general"

def extract_context(query: str) -> Dict[str, str]:
    """
    Extracts year, semester, and course from the query if present.
    """
    context = {}
    year_match = re.search(r"(first|second|third|fourth|\d+)( year)?", query, re.IGNORECASE)
    sem_match = re.search(r"(semester|sem)\s*(\d+)", query, re.IGNORECASE)
    course_match = re.search(r"course\s*([a-zA-Z0-9]+)", query, re.IGNORECASE)
    if year_match:
        context["year"] = year_match.group(0)
    if sem_match:
        context["semester"] = sem_match.group(2)
    if course_match:
        context["course"] = course_match.group(1)
    return context
