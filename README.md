# GenAI Backend Application
 
A production-style dual-service GenAI chat backend demonstrating enterprise backend engineering with AI system integration.
 
## What This System Does
 
Two microservices with clear separation of concerns:
 
- **Backend Service** (Java / Spring Boot) — JWT authentication, REST API, JPA persistence, BOLA enforcement, request tracing
- **GenAI Service** (Python / Flask) — LLM inference, LangGraph agentic workflows, MCP tool execution, RAG pipeline
 
They communicate over HTTP. Spring Boot is the authentication boundary. A single `X-Request-Id` flows across both services for end-to-end log correlation.
 
---
 
## Architecture
 
```
Client
  │
  ▼
Spring Boot :8080
  ├── LoggingFilter      → generates/reuses X-Request-Id, MDC logging
  ├── AuthFilter         → validates JWT, sets SecurityContext
  ├── ChatController     → REST endpoints, reads identity from filter
  ├── ChatService        → business logic, BOLA ownership checks, TX split
  ├── ChatRepository     → JPA + paginated queries
  └── LLMClient          → forwards X-Request-Id + X-User-* headers
          │
          ▼
Flask :5001
  ├── logging_middleware → reuses upstream X-Request-Id (generates fallback UUID if absent)
  ├── auth_middleware    → reads X-User-Id/Email/Role, defaults role to USER if absent
  ├── LLMService         → initialises AgentState, invokes LangGraph
  └── LangGraph StateGraph
        ├── Router Agent    → selects tool
        ├── Policy Agent    → checks role (admin/hr only)
        ├── Executor Agent  → calls MCP tool, max 2 retries
        └── Formatter Agent → builds structured response
              │
              ▼
        MCP Server /tools/run
              │
              ▼
        Tool Registry (deterministic Python functions)
```
 
---
 
## Key Design Decisions
 
| Decision | Why |
|---|---|
| Two services: Java + Python | Java has Spring Security + JPA maturity. Python has LangChain + LangGraph. Each language does what it's best at. |
| Spring is the auth boundary | Spring Security has battle-tested JWT validation. Flask is internal — it trusts forwarded identity headers. |
| Stateless JWT — no server sessions | Any instance can serve any request. Enables horizontal scaling without sticky sessions. |
| HttpOnly cookie for refresh token | localStorage is readable by JS (XSS risk). HttpOnly cookie is not accessible to JavaScript. |
| Split DB transactions around LLM call | LLM calls take 2–10 seconds. Holding a DB transaction open that long exhausts HikariCP's connection pool. TX1 saves prompt, TX2 saves response. |
| BOLA check in service layer | Business rules in the service apply regardless of which controller calls the method. Controller-only checks are easily bypassed by new endpoints. |
| Refresh token hashed with SHA-256 | If DB is leaked, attacker gets hashes, not usable tokens. Same principle as password hashing — never store secrets in plain text. |
| requestId across both services | One ID ties all logs for a single request across both services. AI backends fail at multiple layers — tracing is essential for debugging. |
| LangGraph over a chain | Graph expresses branching explicitly: policy fail → skip executor, retry loops, conditional routing. A chain is linear and cannot express this cleanly. |
| MCP tool boundary | Separates LLM decision ('which tool?') from deterministic execution ('run this function'). Prevents prompt injection from reaching tool execution. |
 
---
 
## Request Flow
 
```
POST /api/chats  Authorization: Bearer <jwt>
  │
  ├── CorsConfig          validates origin, handles preflight
  ├── LoggingFilter       X-Request-Id added to MDC + request log
  ├── AuthFilter          JWT validated → userId/email/role → SecurityContext
  ├── ChatController      reads identity from request attributes
  ├── ChatService         BOLA check → TX1 save prompt → call LLM → TX2 save response
  └── LLMClient           forwards X-Request-Id, X-User-Id, X-User-Email, X-User-Role
          │
          ▼
  POST /infer  (Flask)
    ├── logging_middleware   reuses X-Request-Id (fallback: new UUID)
    ├── auth_middleware      reads X-User-* headers, defaults role to USER if absent
    ├── LLMService           AgentState{trace_id, role, query}
    └── LangGraph            Router → Policy → Executor → Formatter
          │
          ▼
  Structured JSON response with execution_id, trace_id, latency_ms, retry_count
```
 
---
 
## Technology Stack
 
| Layer | Technology |
|---|---|
| Java backend | Spring Boot 3.2, Spring Security, Spring Data JPA, Hibernate |
| Database | H2 (dev) / PostgreSQL (prod), HikariCP connection pooling |
| Auth | JWT (HS256), BCrypt password hashing, HttpOnly refresh cookie |
| AI service | Python 3.x, Flask, LangGraph, pdfplumber |
| Workflow | LangGraph StateGraph, MCP (Model Context Protocol) |
| Observability | MDC structured logging, X-Request-Id tracing across services |
| Build | Maven (Java), pip + venv (Python) |
 
---
 
## Quick Start
 
**Prerequisites:** Java 17+, Python 3.8+, Maven 3.8+
 
```bash
# 1. Start GenAI Service
cd genai-service
python -m venv venv && source venv/bin/activate
pip install -r requirements.txt
echo "OPENAI_API_KEY=your_key_here" > .env
python app.py
 
# 2. Start Backend Service
cd ../backend-service
mvn clean spring-boot:run
```
 
> **Note:** There is no registration endpoint yet. A user must be seeded directly into the database before login will work. See Known Gaps.
 
**Login and create a chat:**
```bash
# Login (requires a seeded user in the database)
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "password"}'
 
# Use the returned access token
curl -X POST http://localhost:8080/api/chats \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <access_token>" \
  -d '{"prompt": "Check eligibility for employee 123"}'
```
 
---
 
## Security Implementation
 
- `POST /auth/login` → verifies BCrypt password, returns JWT access token + HttpOnly refresh cookie
- `POST /auth/refresh` → validates hashed refresh token, rotates token pair
- `POST /auth/logout` → clears cookie, deletes DB token
- `AuthFilter` → validates JWT on every request, populates SecurityContext
- `ChatService` → ownership check: `chat.getUser().getId().equals(jwtUserId)` on every read/update/delete
- `auth_middleware.py` → reads `X-User-*` headers from Spring; defaults role to `USER` if absent
 
---
 
## Request Tracing
 
```
LoggingFilter (Spring)    →  MDC.put("requestId", id)  →  all Spring logs tagged
LLMClient (Spring)        →  forwards X-Request-Id header to Flask
logging_middleware.py     →  reuses X-Request-Id if present, else generates fallback UUID
error_handler.py          →  includes requestId in all error responses
AgentState.trace_id       →  carries requestId into LangGraph workflow
```
 
One `requestId` → grep both Spring and Flask logs together.
 
---
 
## Known Gaps
 
| Gap | Next Step |
|---|---|
| No user registration endpoint | Add `POST /auth/register` with email validation |
| No automated tests | JUnit + Mockito for ChatService, @SpringBootTest for auth flow |
| LangGraph router is deterministic | Replace with LLM-based routing + Pydantic validation |
| FAISS is in-memory (does not persist) | Replace with pgvector — same PostgreSQL, HNSW index |
| No RAGAS evaluation | Add `eval/ragas_eval.py` — faithfulness, relevancy, context precision |
| No LangSmith tracing | 3 env vars: `LANGCHAIN_TRACING_V2`, `LANGCHAIN_API_KEY`, `LANGCHAIN_PROJECT` |
| LoggingFilter does not measure duration | Add `startTime = System.currentTimeMillis()` before chain, log `elapsed` after |
 
---
 
## Project Structure
 
```
genai-backend-app/
├── backend-service/
│   ├── src/main/java/com/example/backend/
│   │   ├── controllers/     ChatController, AuthController
│   │   ├── services/        ChatService, AuthService
│   │   ├── repositories/    ChatRepository, UserRepository
│   │   ├── models/          ChatMessage, User, RefreshToken
│   │   ├── dto/             CreateChatRequest, LoginRequest, UpdateChatRequest
│   │   ├── config/          SecurityConfig, CorsConfig, AsyncConfig
│   │   ├── exceptions/      UnauthorizedException, ForbiddenException, ResourceNotFoundException
│   │   ├── filters/         LoggingFilter, AuthFilter
│   │   └── BackendServiceApplication.java
│   └── src/main/resources/application.yml
├── genai-service/
│   ├── app.py
│   ├── agentic_mcp/         graph.py, agents.py, state.py, mcp_client.py, mcp_server.py, tools.py
│   ├── handlers/            llm_handler.py
│   ├── middleware/          logging_middleware.py, auth_middleware.py, error_handler.py
│   ├── services/            llm_service.py
│   ├── repositories/        chat_repository.py, vector_repository.py
│   └── rag/                 rag_pipeline.py
└── README.md
```