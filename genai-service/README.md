# GenAI Service

Flask-based microservice responsible for LLM inference and RAG (Retrieval Augmented Generation).

## Purpose
- Receive prompts from backend-service
- Perform RAG (retrieve relevant context + generate response)
- Return generated text to backend-service
- Keep heavy AI compute separate from the main API

## Tech Stack
- Python 3.9+
- Flask
- (LLM library here, e.g., LangChain, OpenAI SDK, or HuggingFace)
- Vector store (e.g., FAISS, Pinecone, Chroma)

## Architecture
- Lightweight Flask app
- Endpoints called internally by backend-service
- Main responsibility: Prompt → Context Retrieval → LLM Call → Response

## Key Workflow: Response Generation
1. Receive prompt from backend-service
2. Retrieve relevant documents (RAG)
3. Augment prompt with context
4. Call LLM (e.g., OpenAI, Llama, etc.)
5. Return generated response

## Setup & Run
```bash
Python ≥ 3.9 (python --version)

cd genai-service
python -m venv venv
source venv/bin/activate    # Windows: venv\Scripts\activate

pip install -r requirements.txt
python app.py
