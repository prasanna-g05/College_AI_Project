import os
import sys
import time
from typing import List
from rich.console import Console
from rich.table import Table
from rich.prompt import Prompt
from rich.panel import Panel
from loguru import logger
from rag_chatbot import RAGChatbot
from config import CHROMA_DB_DIR, EMBEDDING_MODEL

console = Console()

class TerminalChatbotApp:
    def __init__(self):
        self.chatbot = RAGChatbot(CHROMA_DB_DIR, EMBEDDING_MODEL)
        self.query_count = 0
        self.start_time = time.time()

    def print_welcome(self):
        console.print(Panel("[bold green]Welcome to the PCE IT Department Educational Chatbot![/bold green]\n"
                            "Type your educational query or use commands: [bold]/upload[/bold], [bold]/help[/bold], [bold]/stats[/bold], [bold]/quit[/bold]",
                            title="PCE Nagpur IT Chatbot", expand=False))

    def print_help(self):
        console.print(Panel("""
[bold]/upload[/bold] - Upload PDF documents for RAG retrieval
[bold]/help[/bold]   - Show this help message
[bold]/stats[/bold]  - Show usage statistics
[bold]/quit[/bold]   - Exit the chatbot
Simply type your question to get an answer.
        """, title="Help", expand=False))

    def print_stats(self):
        elapsed = time.time() - self.start_time
        console.print(Panel(f"[bold]Queries answered:[/bold] {self.query_count}\n[bold]Uptime:[/bold] {elapsed:.1f} seconds",
                            title="Stats", expand=False))

    def upload_pdfs(self):
        pdf_paths = []
        console.print("Enter PDF file paths to upload (comma-separated):", style="cyan")
        paths = Prompt.ask("PDF paths").split(",")
        for path in paths:
            path = path.strip()
            if os.path.isfile(path) and path.lower().endswith(".pdf"):
                pdf_paths.append(path)
            else:
                console.print(f"[red]File not found or not a PDF:[/red] {path}")
        if pdf_paths:
            try:
                num_chunks = self.chatbot.process_pdfs(pdf_paths)
                console.print(f"[green]Processed {len(pdf_paths)} PDFs into {num_chunks} chunks.[/green]")
            except Exception as e:
                logger.error(f"Error processing PDFs: {e}")
                console.print(f"[red]Failed to process uploaded PDFs.[/red]")
        else:
            console.print("[yellow]No valid PDFs provided.[/yellow]")

    def handle_query(self, query: str):
        t0 = time.time()
        response = self.chatbot.answer_query(query)
        t1 = time.time()
        self.query_count += 1
        answer = response.get("answer", "[red]No answer available.[/red]")
        qtype = response.get("type", "unknown")
        context = response.get("context", {})
        docs = response.get("docs")
        console.print(Panel(f"[bold]Type:[/bold] {qtype}\n[bold]Context:[/bold] {context}\n[bold]Answer:[/bold] {answer}", title="Response", expand=False))
        if docs:
            table = Table(title="Retrieved Document Chunks", show_lines=True)
            table.add_column("Chunk", style="dim", width=80)
            for i, chunk in enumerate(docs, 1):
                table.add_row(chunk[:500] + ("..." if len(chunk) > 500 else ""))
            console.print(table)
        console.print(f"[blue]Response time: {t1-t0:.2f} seconds[/blue]")

    def run(self):
        self.print_welcome()
        while True:
            try:
                user_input = Prompt.ask("[bold green]You[/bold green]").strip()
                if not user_input:
                    continue
                if user_input.lower() in ["/quit", "quit", "exit"]:
                    console.print("[bold yellow]Goodbye![/bold yellow]")
                    break
                elif user_input.lower() in ["/help", "help"]:
                    self.print_help()
                elif user_input.lower() in ["/stats", "stats"]:
                    self.print_stats()
                elif user_input.lower() in ["/upload", "upload"]:
                    self.upload_pdfs()
                else:
                    self.handle_query(user_input)
            except (KeyboardInterrupt, EOFError):
                console.print("\n[bold yellow]Session ended by user.[/bold yellow]")
                break
            except Exception as e:
                logger.error(f"Unexpected error: {e}")
                console.print(f"[red]An error occurred: {e}[/red]")

if __name__ == "__main__":
    TerminalChatbotApp().run() 