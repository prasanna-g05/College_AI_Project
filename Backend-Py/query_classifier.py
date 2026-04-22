from typing import Literal, Dict
import re

PDF_KEYWORDS = [
    "syllabus", "timetable", "faculty", "faculty details", "course content",
    "semester subjects", "hod", "department", "project", "workflow", "student portal",
    "notes", "unit", "pdf", "document", "file"
]

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

DOWNLOAD_INTENT_KEYWORDS = [
    "send me", "give me", "download", "provide me", "get me", "share",
    "i need", "can you send", "show me file", "show me pdf", "show me document",
    "send", "send pdf", "send file"
]

QueryType = Literal["pdf", "general", "non-educational"]


def classify_query(query: str) -> QueryType:
    """
    Classifies the query as 'pdf', 'general', or 'non-educational'.
    Gives priority to document-related intent when download/file language is present.
    """
    q = query.lower().strip()

    has_download_intent = any(k in q for k in DOWNLOAD_INTENT_KEYWORDS)
    has_content_intent = any(k in q for k in GENERAL_EDU_KEYWORDS)
    has_pdf_keywords = any(k in q for k in PDF_KEYWORDS)
    has_non_edu = any(k in q for k in NON_EDU_KEYWORDS)

    if has_non_edu and not has_pdf_keywords and not has_content_intent and not has_download_intent:
        return "non-educational"

    if has_download_intent:
        return "pdf"

    if has_content_intent and has_pdf_keywords:
        return "pdf"

    if has_pdf_keywords:
        return "pdf"

    if has_content_intent:
        return "general"

    return "general"


def extract_context(query: str) -> Dict[str, str]:
    """
    Extracts year, semester, and course from the query if present.
    """
    context = {}
    q = query.strip()

    year_match = re.search(
        r"\b(first|second|third|fourth|1st|2nd|3rd|4th|\d+)\s*year\b",
        q,
        re.IGNORECASE
    )

    sem_match = re.search(
        r"\b(?:semester|sem)\s*(\d+)\b",
        q,
        re.IGNORECASE
    )

    course_match = re.search(
        r"\bcourse\s+([a-zA-Z0-9\-_]+)\b",
        q,
        re.IGNORECASE
    )

    if year_match:
        context["year"] = year_match.group(0)

    if sem_match:
        context["semester"] = sem_match.group(1)

    if course_match:
        context["course"] = course_match.group(1)

    return context
