# GenAI Backend Application

A production-style backend for a GenAI chat application with RAG support. The project is split into two services for clear separation of concerns.

- **backend-service** → Spring Boot REST API (chat management, persistence, auth)
- **genai-service** → Flask service (LLM inference + RAG logic)

The backend-service exposes REST APIs and orchestrates calls to the genai-service for AI response generation. Chat history is persisted in a relational database.

## Project Structure

genai-backend-app/
├── backend-service/          # Spring Boot API
├── genai-service/            # Flask GenAI service
└── README.md


## Quick Start
- See `backend-service/README.md` for Java setup and API usage
- See `genai-service/README.md` for Python/Flask setup and LLM integration
