# College_AI_Project

## 📘 Overview
**College AI Project** is an intelligent assistant platform designed to support students and staff using AI-powered conversations, document retrieval, and academic information access.  
It combines a **FastAPI backend**, **Android frontend**, **RAG pipeline**, and **role-based modules** to create a practical education-focused assistant system. [cite:1734][cite:1735]

This project demonstrates knowledge of:
- Artificial Intelligence (AI) and practical LLM integration
- Retrieval-Augmented Generation (RAG)
- LangChain-based orchestration
- FastAPI backend development
- Android app development with Kotlin and Jetpack Compose
- Authentication and role-based access control
- Database and vector-store integration

---

## ✨ Features

- **AI Chatbot Assistant** – Answers student queries using natural language processing and retrieved context. [cite:1734][web:1831]
- **RAG Pipeline** – Retrieves relevant content from stored documents and files to improve answer accuracy. [web:1831][web:1834]
- **LangChain Integration** – Supports structured AI workflows and retrieval-based conversations. [web:1834][web:1837]
- **FastAPI Backend** – Provides scalable APIs for chatbot, authentication, student/admin actions, and academic modules. [web:1839]
- **JWT Authentication** – Secures access and supports session-based user validation. [web:1830][web:1839]
- **Admin Module** – Allows admin-side management of platform data and academic resources. [cite:1735]
- **Student Module** – Allows students to access academic information, resources, and AI support tools. [cite:1735]
- **TNP Module** – Includes company details, notes, PYQs, and mock tests for training and placement preparation. [cite:1824]
- **Database Support** – Stores user, company, and academic data persistently. [cite:1824]

---

## 🛠️ Tech Stack

### Languages
- Python
- Kotlin

### Frameworks & Libraries
- FastAPI
- LangChain
- Jetpack Compose
- Retrofit / OkHttp
- OpenAI API / Gemini API

### Databases & Tools
- SQLite / MySQL
- ChromaDB (vector storage)
- GitHub
- Android Studio

### Concepts Used
- Retrieval-Augmented Generation (RAG)
- Agentic AI workflows
- REST API development
- Authentication with JWT
- Role-based access control
- Mobile + backend integration

---

## 📱 Modules

### Student Side
- View academic/company-related information
- Access notes, PYQs, and mock tests
- Interact with AI chatbot assistant

### Admin Side
- Manage platform resources
- Upload and organize TNP content
- Maintain company-specific preparation materials

---

## ⚙️ Setup

### Backend
1. Clone the repository
2. Create a virtual environment
3. Install dependencies
4. Run the FastAPI server

```bash
pip install -r requirements.txt
uvicorn api_server:app --reload
```

### Frontend
1. Open the Android project in Android Studio
2. Sync Gradle files
3. Run the app on emulator or device

---

## 🎯 Purpose

The goal of this project is to explore how AI can be integrated into college systems to improve access to information, simplify academic workflows, and provide a smarter support experience for students and staff.

---

## 📌 Note
This project is still evolving, and new features or refinements may be added over time.
