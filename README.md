# GenAI Backend Application

A production-style backend for a GenAI chat application with RAG support, built with microservices architecture for clear separation of concerns.

## Overview

This project implements a comprehensive GenAI chat system with the following key features:
- **RESTful API** for chat management with full CRUD operations
- **Layered architecture** with proper separation of concerns
- **Database persistence** with ACID transactions and optimized queries
- **Agentic workflows** using LangGraph for deterministic AI orchestration
- **Model Context Protocol (MCP)** for tool-based AI interactions
- **RAG (Retrieval Augmented Generation)** capabilities
- **Production-ready features** including logging, error handling, authentication, and observability

## Architecture

The application is split into two main services:

### Backend Service (Java/Spring Boot)
- **Port**: 8080
- **Technology**: Spring Boot 3.2.0, Java 17, Spring Data JPA, H2/PostgreSQL
- **Responsibilities**:
  - REST API endpoints for chat CRUD operations
  - User authentication and authorization
  - Database persistence and transaction management
  - Orchestration of GenAI service calls
  - Request/response logging and error handling

### GenAI Service (Python/Flask)
- **Port**: 5001
- **Technology**: Flask, LangGraph, Python 3.x
- **Responsibilities**:
  - LLM inference and response generation
  - Agentic workflow orchestration using state machines
  - MCP tool execution for deterministic operations
  - RAG pipeline for document processing
  - Chat history management

## Project Structure

```
genai-backend-app/
├── backend-service/          # Spring Boot REST API
│   ├── src/main/java/com/example/backend/
│   │   ├── controllers/      # REST controllers
│   │   ├── services/         # Business logic
│   │   ├── repositories/     # Data access layer
│   │   ├── models/           # JPA entities
│   │   ├── dto/              # Data transfer objects
│   │   ├── config/           # Configuration classes
│   │   ├── exceptions/       # Global exception handling
│   │   ├── filters/          # Request filters
│   │   └── BackendServiceApplication.java
│   ├── src/main/resources/
│   │   └── application.yml   # Application configuration
│   └── pom.xml               # Maven dependencies
├── genai-service/            # Flask GenAI service
│   ├── app.py                # Flask application entry point
│   ├── agentic_mcp/          # LangGraph workflow components
│   │   ├── graph.py          # StateGraph definition
│   │   ├── agents.py         # Agent implementations
│   │   ├── state.py          # State definitions
│   │   ├── mcp_client.py     # MCP client
│   │   ├── mcp_server.py    # MCP server blueprint
│   │   └── tools.py          # Tool registry and implementations
│   ├── handlers/             # Flask blueprints
│   ├── middleware/           # Flask middleware
│   ├── services/             # Business services
│   ├── repositories/         # Data repositories
│   ├── rag/                  # RAG pipeline
│   └── requirements.txt      # Python dependencies
└── README.md
```

## Key Features

### Backend Service Features
- **REST API Design**: Proper HTTP methods, status codes, and resource modeling
- **Layered Architecture**: Controllers → Services → Repositories with clear separation
- **Database Design**: Normalized schema with strategic denormalization for performance
- **Transaction Management**: ACID compliance for chat creation workflow
- **Authentication**: Bearer token validation via request filters
- **Pagination & Sorting**: Efficient data retrieval with Spring Data
- **Error Handling**: Global exception handler with structured error responses
- **Logging**: Structured JSON logs with request tracing
- **CORS Support**: Configurable cross-origin resource sharing

### GenAI Service Features
- **Agentic Workflows**: Deterministic AI orchestration using LangGraph state machines
- **MCP Integration**: Tool-based interactions with business logic separation
- **Multi-Agent System**: Router, Policy, Executor, and Formatter agents
- **RAG Pipeline**: Document processing and context retrieval
- **Tool Registry**: Extensible tool system for business operations
- **State Management**: Typed state definitions for workflow reliability
- **Error Handling**: Fail-fast design with bounded retries

## Technology Stack

### Backend Service
- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: H2 (dev) / PostgreSQL (prod)
- **ORM**: Hibernate/JPA
- **Security**: Spring Security
- **Validation**: Jakarta Bean Validation
- **Logging**: Logback with Logstash encoder
- **Build Tool**: Maven
- **Testing**: JUnit 5, Spring Boot Test

### GenAI Service
- **Framework**: Flask
- **Language**: Python 3.x
- **Workflow Engine**: LangGraph
- **HTTP Client**: Requests
- **CORS**: Flask-CORS
- **Document Processing**: PDFPlumber
- **Environment**: python-dotenv

## Quick Start

### Prerequisites
- Java 17 or higher
- Python 3.8 or higher
- Maven 3.8 or higher
- Git

### Running the Application

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd genai-backend-app
   ```

2. **Start the GenAI Service**
   ```bash
   cd genai-service
   python -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   pip install -r requirements.txt
   # Set OPENAI_API_KEY environment variable
   python app.py
   ```

3. **Start the Backend Service**
   ```bash
   cd ../backend-service
   mvn clean spring-boot:run
   ```

### API Testing

Create a chat message:
```bash
curl -X POST http://localhost:8080/api/chats \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -d '{"userId": "user123", "prompt": "What is AI?"}'
```

Get chat history:
```bash
curl -X GET "http://localhost:8080/api/chats?userId=user123" \
  -H "Authorization: Bearer test-token"
```

## Development

### Backend Service Development
- See `backend-service/README.md` for detailed API documentation
- Run tests: `mvn test`
- Build JAR: `mvn clean package`
- Check code style: Integrated with Spring Boot standards

### GenAI Service Development
- See `genai-service/README.md` for detailed API documentation
- Run with debug: `python app.py` (debug=True)
- Test endpoints using the provided curl examples
- Add new tools in `agentic_mcp/tools.py`

## Configuration

### Backend Service Configuration
Key settings in `application.yml`:
- Database connection (H2 for dev, PostgreSQL for prod)
- CORS allowed origins
- GenAI service URL
- Async thread pool configuration
- JPA/Hibernate settings

### GenAI Service Configuration
Environment variables:
- `OPENAI_API_KEY`: Required for LLM operations
- Flask configuration through `app.py`

## Deployment

### Backend Service Deployment
- Build JAR: `mvn clean package`
- Run: `java -jar target/backend-service-0.0.1-SNAPSHOT.jar`
- Configure PostgreSQL for production
- Set up reverse proxy (nginx) for production deployment

### GenAI Service Deployment
- Use Gunicorn for production: `gunicorn -w 4 app:app`
- Configure environment variables
- Set up proper logging and monitoring

## Contributing

1. Follow the existing code structure and naming conventions
2. Add tests for new features
3. Update documentation for API changes
4. Ensure all tests pass before submitting PR

## License

This project is licensed under the MIT License - see the LICENSE file for details.
