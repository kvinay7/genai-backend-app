# Backend Service
A production-oriented backend service demonstrating HTTP fundamentals, REST design, layered architecture, concurrency handling, and distributed system best practices.

## Purpose
- Expose RESTful endpoints for chat CRUD operations
- Enforces stateless authentication via Authorization header
- Persist chat history (centralized source of truth)
- Orchestrate calls to GenAI service for LLM responses
- Return structured responses with proper HTTP status codes

## Responsibilities
  - Reliability: Global exception handling, proper status codes
  - Scalability: Stateless APIs, pagination, connection pooling
  - Fault Tolerance: Graceful error responses for downstream failures
  - Security Boundary: Authentication via headers (never exposing secrets to frontend)
  - State Centralization: Persistent chat history in database
  - Compute Offloading: LLM inference executed in separate Python service
  - Observability: Structured JSON logs with request tracing, health endpoint

## Tech Stack
- Java + Spring Boot
- Spring Data JPA + Hibernate
- H2 Database (dev) / PostgreSQL (prod)

## Architecture
- **Layered Architecture**:
  - Controllers → HTTP-only responsibilities (routing, binding, validation, status codes, pagination and sorting)
  - Services → Pure business logic, orchestration of LLM service calls, HTTP-agnostic
  - Repositories → Data access only, no business logic
  - Global Exception Handler → Centralized error handling with requestId
  - Filters/Middlewares → Cross-cutting concerns executed before/after handlers (Request Tracing & Authentication)
  - Config → CORS (configurable origins), Security (CSRF disabled, stateless), Logging (programmatic JSON setup)

- **Database Design**
  - Main entity: `ChatMessage`
  - Relationship: One User → Many ChatMessages (1:N)
  - Primary Key: `id`
  - Foreign Key: `user_id`
  - Composite Index: `(user_id, created_at DESC)` for fast chat history retrieval

- **Process & Thread Management**:
  - The application runs as **two separate OS processes**:
    - `backend-service` → One JVM process (Spring Boot)
    - `genai-service` → One Python process (Flask/Gunicorn)

  - Each process contains **multiple threads**:
    - Spring Boot uses Tomcat’s default thread pool (200 threads) to handle concurrent HTTP requests.
    - Long-running LLM inference is offloaded to async threads so the main HTTP thread is never blocked.
      
## Setup & Run
```bash

Java ≥ 17 (java --version)
Maven ≥ 3.8 (mvn --version)

cd backend-service
mvn clean spring-boot:run
```

## API Endpoints

### 1. Create Chat Message

**Request:**
```bash
curl -X POST http://localhost:8080/api/chats \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -d '{
    "userId": "user123",
    "prompt": "What is Retrieval Augmented Generation?"
  }'
```

**Response (201 Created):**
```json
{
  "id": 1,
  "userId": "user123",
  "prompt": "What is Retrieval Augmented Generation?",
  "response": "RAG is a technique that combines retrieval and generation...",
  "createdAt": "2026-02-09T10:30:45.123456Z",
  "updatedAt": null
}
```

---

### 2. Get All Chats for User

**Request:**
```bash
curl -X GET "http://localhost:8080/api/chats?userId=user123&page=0&size=10&sortBy=createdAt&direction=DESC" \
  -H "Authorization: Bearer test-token"
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 2,
      "userId": "user123",
      "prompt": "Explain vector databases",
      "response": "Vector databases store embeddings...",
      "createdAt": "2026-02-09T10:35:20.654321Z",
      "updatedAt": null
    },
    {
      "id": 1,
      "userId": "user123",
      "prompt": "What is Retrieval Augmented Generation?",
      "response": "RAG is a technique that combines retrieval and generation...",
      "createdAt": "2026-02-09T10:30:45.123456Z",
      "updatedAt": null
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalPages": 1,
  "totalElements": 2,
  "last": true,
  "size": 10,
  "number": 0,
  "sort": {
    "empty": false,
    "sorted": true,
    "unsorted": false
  },
  "numberOfElements": 2,
  "first": true,
  "empty": false
}
```

---

### 3. Get Chat by ID

**Request:**
```bash
curl -X GET http://localhost:8080/api/chats/1 \
  -H "Authorization: Bearer test-token"
```

**Response (200 OK):**
```json
{
  "id": 1,
  "userId": "user123",
  "prompt": "What is Retrieval Augmented Generation?",
  "response": "RAG is a technique that combines retrieval and generation...",
  "createdAt": "2026-02-09T10:30:45.123456Z",
  "updatedAt": null
}
```

**Response (404 Not Found):**
```json
{
  "error": "Chat not found"
}
```

---

### 4. Update Chat (Full Update)

**Request:**
```bash
curl -X PUT http://localhost:8080/api/chats/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -d '{
    "prompt": "What is RAG (updated)?",
    "response": "RAG combines retrieval and generation models (updated)..."
  }'
```

**Response (200 OK):**
```json
{
  "id": 1,
  "userId": "user123",
  "prompt": "What is RAG (updated)?",
  "response": "RAG combines retrieval and generation models (updated)...",
  "createdAt": "2026-02-09T10:30:45.123456Z",
  "updatedAt": "2026-02-09T11:45:30.987654Z"
}
```

---

### 5. Partial Update Chat

**Request:**
```bash
curl -X PATCH http://localhost:8080/api/chats/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -d '{
    "response": "Updated response only..."
  }'
```

**Response (200 OK):**
```json
{
  "id": 1,
  "userId": "user123",
  "prompt": "What is Retrieval Augmented Generation?",
  "response": "Updated response only...",
  "createdAt": "2026-02-09T10:30:45.123456Z",
  "updatedAt": "2026-02-09T11:50:00.111111Z"
}
```

---

### 6. Delete Chat by ID

**Request:**
```bash
curl -X DELETE http://localhost:8080/api/chats/1 \
  -H "Authorization: Bearer test-token"
```

**Response (204 No Content):**
```
[Empty body]
```

---

### 7. Delete All Chats for User

**Request:**
```bash
curl -X DELETE "http://localhost:8080/api/chats?userId=user123" \
  -H "Authorization: Bearer test-token"
```

**Response (204 No Content):**
```
[Empty body]
```

---

## Error Responses

### 400 Bad Request
```json
{
  "timestamp": "2026-02-09T10:30:45.123456Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid request parameters"
}
```

### 401 Unauthorized
```json
{
  "timestamp": "2026-02-09T10:30:45.123456Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Missing or invalid authorization token"
}
```

### 500 Internal Server Error
```json
{
  "timestamp": "2026-02-09T10:30:45.123456Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred"
}
```
---

