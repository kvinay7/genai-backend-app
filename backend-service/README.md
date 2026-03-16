# Backend Service
 
Spring Boot REST API — authentication boundary, chat management, JPA persistence, and request orchestration for the GenAI backend application.
 
---
 
## What This Service Owns
 
| Responsibility | Implementation |
|---|---|
| User authentication | JWT generation + validation, BCrypt password hashing, refresh token rotation |
| Authorization | BOLA ownership checks in service layer, RBAC via JWT role claim |
| REST API | CRUD for chat messages — `GET /api/chats`, `POST`, `PUT`, `DELETE` |
| Persistence | Spring Data JPA + Hibernate, normalized schema, HikariCP connection pooling |
| Transaction management | Split TX: save prompt → LLM call → save response (keeps transactions short) |
| Tracing | MDC `requestId`, forwarded to Flask as `X-Request-Id` header |
| Error handling | Structured error responses — 401, 403, 404, 400, 500 with timestamp + requestId |
 
---
 
## Request Processing Pipeline
 
Every request passes through this pipeline in order:
 
```
1. CorsConfig          → validates origin, handles OPTIONS preflight
2. LoggingFilter       → reads or generates X-Request-Id, MDC.put(), logs method + path
3. AuthFilter          → validates JWT, extracts userId/email/role, sets SecurityContext
4. SecurityConfig      → permits /auth/**, /actuator/health; secures everything else
5. ChatController      → routes to handler; @Valid on createChat only (updateChat has no constraints)
6. ChatService         → BOLA ownership check, business logic, TX split
7. ChatRepository      → JPA query, pagination, index-backed retrieval
8. LLMClient           → forwards requestId + identity headers to Flask :5001
9. LoggingFilter       → logs completion with method, path, and status code
```
 
---
 
## Authentication Flow
 
### Login
```
POST /auth/login  {email, password}
  → AuthService.login()
  → UserRepository.findByEmail()
  → BCryptPasswordEncoder.matches(rawPassword, storedHash)
  → JWT generated with userId, email, role claims (HS256, 15 min expiry)
  → Refresh token: random UUID stored as SHA-256 hash in refresh_tokens table
  → Response: { accessToken: "..." }
  → Cookie: refreshToken=<token>; HttpOnly; Secure; SameSite=Strict; Path=/auth
```
 
### Request Authentication
```
GET /api/chats  Authorization: Bearer <jwt>
  → AuthFilter.doFilterInternal()
  → JWT signature + expiry validated
  → Claims extracted: userId, email, role
  → SecurityContextHolder populated
  → request.setAttribute("userId", userId)
  → request.setAttribute("role", role)
  → request.setAttribute("requestId", requestId)
  → Controller reads identity from these attributes — never from request body
```
 
### Refresh
```
POST /auth/refresh  (sends HttpOnly cookie automatically)
  → SHA-256 hash of cookie value
  → RefreshToken entity looked up by token hash
  → Expiry checked
  → New access token + new refresh token generated
  → Old refresh token deleted (rotation)
  → New pair returned
```
 
**Why hash the refresh token?**
If the database is leaked, the attacker gets SHA-256 hashes, not usable tokens. Refresh tokens are high-entropy random UUIDs so SHA-256 without salt is acceptable here — unlike passwords which need BCrypt's adaptive cost factor.
 
**Why HttpOnly cookie for refresh token?**
localStorage is readable by JavaScript. An XSS attack can steal tokens from localStorage. HttpOnly cookies are invisible to JS. The `SameSite=Strict` flag mitigates CSRF. `allowCredentials(true)` in CorsConfig is required for the browser to send the cookie cross-origin.
 
---
 
## Authorization — BOLA Prevention
 
Broken Object Level Authorization (BOLA/IDOR): a user accesses another user's resource by guessing IDs.
 
**Where the check lives:** `ChatService.java` — not the controller.
 
```java
// ChatService.java — every read, update, and delete operation includes this check
ChatMessage chat = chatRepository.findById(chatId)
    .orElseThrow(() -> new ResourceNotFoundException("Chat not found"));
 
if (!chat.getUser().getId().equals(jwtUserId)) {
    throw new ForbiddenException("Access denied");
}
```
 
**Why `chat.getUser().getId()` and not `chat.getUserId()`?**
The `ChatMessage` entity holds a `@ManyToOne User user` relationship. The user ID is accessed via the relationship object, not a denormalized field. This follows the JPA entity model where relationships are navigated, not bypassed.
 
**Why service layer, not controller?**
If a new endpoint is added tomorrow and the developer forgets to add an authorization check to the controller, the service-layer check still protects the data. Business rules belong in the service.
 
**Admin override:** Admin role can delete any chat, but that override is implemented separately in `deleteChat(...)`. The shared ownership helper itself checks only ownership.
 
---
 
## Database Design
 
### Schema
 
```sql
CREATE TABLE users (
    id       BIGINT PRIMARY KEY AUTO_INCREMENT,
    email    VARCHAR(255) NOT NULL UNIQUE,
    name     VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,  -- BCrypt hash, never plain text
    role     VARCHAR(50)  NOT NULL   -- e.g. USER, ADMIN
);
 
CREATE TABLE refresh_tokens (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    token_hash VARCHAR(255) NOT NULL,  -- SHA-256 of actual token
    expires_at TIMESTAMP NOT NULL
);
 
CREATE TABLE chat_messages (
    id        BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id   BIGINT NOT NULL REFERENCES users(id),
    prompt    TEXT NOT NULL,
    response  TEXT,
    created_at TIMESTAMP NOT NULL,
    user_name VARCHAR(255)   -- denormalized for read performance
);
 
-- Composite index used by the entity mapping
CREATE INDEX idx_user_created_desc ON chat_messages(user_id, created_at DESC);
```
 
> **Note:** The current entity includes `created_at` via `createdAt`, but does not currently include a separate `updated_at` field or Spring Data auditing annotations.
 
### Design Decisions
 
**Normalization (3NF):** Users and chat_messages are separate tables. No redundancy — user data stored once, referenced by foreign key.
 
**Strategic denormalization:** `user_name` is stored in `chat_messages` to avoid a JOIN on every chat history display. Chat history is read far more than written. This is a deliberate trade-off.
 
**Why split transactions around the LLM call?**
 
```
TX1: INSERT INTO chat_messages (prompt) → COMMIT   // DB connection released
     [LLM call — 2 to 10 seconds]
TX2: UPDATE chat_messages SET response = ? → COMMIT  // DB connection acquired again
```
 
Holding an open DB transaction during the LLM call would occupy a HikariCP connection for the full duration. With a pool of 50 connections and LLM calls averaging 5 seconds, throughput collapses under moderate load. The split keeps each transaction under 10ms.
 
**Risk:** if the LLM call fails between TX1 and TX2, the chat has a prompt with no response. Mitigation: retry at HTTP level or add a `status` column (`PENDING`/`COMPLETE`/`FAILED`).
 
---
 
## REST API

```
GET    /api/chats          → paginated chat history for authenticated user
GET    /api/chats/{id}     → single chat (ownership check)
POST   /api/chats          → create chat — accepts {prompt} only; userId from JWT
PUT    /api/chats/{id}     → full update (ownership check)
DELETE /api/chats/{id}     → delete by ID (ownership check, admin override)
DELETE /api/chats          → delete all chats for authenticated user
 
POST   /auth/login         → authenticate, receive access token + refresh cookie
POST   /auth/refresh       → rotate refresh token, receive new access token
POST   /auth/logout        → clear refresh cookie, revoke DB token
 
GET    /actuator/health    → {status: UP}
```
 
**Why does `CreateChatRequest` not include `userId`?**
The client must never supply their own identity. `userId` comes from the validated JWT claim set by `AuthFilter`. Accepting `userId` from the request body would allow any user to claim any identity.
 
**Status codes:**
- `POST /api/chats` → `201 Created`
- `DELETE` → `204 No Content`
- `GET` / `PUT` → `200 OK`
- Invalid auth → `401 Unauthorized`
- Ownership failure → `403 Forbidden`
- Missing resource → `404 Not Found`
- Validation failure → `400 Bad Request` with field-level errors

### Example Requests And Responses

#### `POST /auth/login`

Request:
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password"}'
```

Response body:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

Response headers include:
```text
Set-Cookie: refresh_token=<uuid>; Path=/auth; HttpOnly; Secure; SameSite=Strict
```

#### `POST /auth/refresh`

Request:
```bash
curl -X POST http://localhost:8080/auth/refresh \
  -H "Cookie: refresh_token=<refresh_token>"
```

Response body:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

#### `POST /api/chats`

Request:
```bash
curl -X POST http://localhost:8080/api/chats \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <access_token>" \
  -d '{"prompt":"Check eligibility for employee 123"}'
```

Response:
```json
{
  "id": 1,
  "prompt": "Check eligibility for employee 123",
  "response": "{response={execution_id=..., status=failed, error=Unauthorized access, retry_count=0, trace_id=...}}",
  "createdAt": "2026-03-16T16:00:00",
  "userName": "User"
}
```

> **Note:** `ChatMessage.response` currently stores the GenAI service payload as a stringified object, not a strongly typed nested JSON structure.

#### `GET /api/chats`

Request:
```bash
curl -X GET "http://localhost:8080/api/chats?page=0&size=10&sortBy=createdAt&direction=DESC" \
  -H "Authorization: Bearer <access_token>"
```

Response:
```json
{
  "content": [
    {
      "id": 1,
      "prompt": "Check eligibility for employee 123",
      "response": "{response={execution_id=..., status=failed, error=Unauthorized access, retry_count=0, trace_id=...}}",
      "createdAt": "2026-03-16T16:00:00",
      "userName": "User"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 1,
  "totalPages": 1
}
```

#### `PUT /api/chats/{id}`

Request:
```bash
curl -X PUT http://localhost:8080/api/chats/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <access_token>" \
  -d '{"prompt":"Updated prompt","response":"Updated response"}'
```

Response:
```json
{
  "id": 1,
  "prompt": "Updated prompt",
  "response": "Updated response",
  "createdAt": "2026-03-16T16:00:00",
  "userName": "User"
}
```

#### `DELETE /api/chats/{id}`

Request:
```bash
curl -X DELETE http://localhost:8080/api/chats/1 \
  -H "Authorization: Bearer <access_token>"
```

Response:
```text
204 No Content
```

---

## Request Tracing
 
```java
// LoggingFilter.java
String requestId = Optional.ofNullable(request.getHeader("X-Request-Id"))
    .orElseGet(() -> UUID.randomUUID().toString());
 
MDC.put("requestId", requestId);
response.setHeader("X-Request-Id", requestId);
 
// logs method, URI, and status — duration tracking not yet implemented
try {
    filterChain.doFilter(request, response);
    log.info("Completed {} {} status={}", method, uri, status);
} finally {
    MDC.clear();
}
```
 
`LLMClient` forwards the same ID to Flask:
```java
headers.set("X-Request-Id", requestId);
headers.set("X-User-Id",    String.valueOf(userId));
headers.set("X-User-Email", email);
headers.set("X-User-Role",  role);
```
 
Result: one `requestId` lets you grep logs across both services to trace any request end-to-end.
 
> **Current gap:** `LoggingFilter` logs method, path, and status but does not measure request duration. To add: capture `startTime = System.currentTimeMillis()` before `filterChain.doFilter()` and log `elapsed = System.currentTimeMillis() - startTime` after.
 
---
 
## Exception Handling
 
```java
// GlobalExceptionHandler.java
UnauthorizedException           → 401  { timestamp, status: 401, error, message, requestId }
ForbiddenException              → 403  { timestamp, status: 403, error, message, requestId }
ResourceNotFoundException       → 404  { timestamp, status: 404, error, message, requestId }
MethodArgumentNotValidException → 400  { timestamp, status: 400, error, message: "<combined field errors>", requestId }
Exception                       → 500  { timestamp, status: 500, error, message: "Internal error", requestId }
```
 
> **Note:** `MethodArgumentNotValidException` returns a single `message` string containing the validation errors — not a structured `errors: [{field, message}]` array. `UpdateChatRequest` also has no bean-validation constraints, so `@Valid` on update operations would have no effect; only `createChat` uses `@Valid` with validated fields.
 
---
 
## Configuration
 
```yaml
# application.yml (key settings)
jwt:
  secret: ${JWT_SECRET}              # from environment variable — never hardcoded
  access-token-expiry-ms: 900000     # 15 minutes
 
spring:
  task:
    execution:
      pool:
        core-size: 10       # configured for @Async — LLM calls currently synchronous
        max-size: 50        # burst capacity
        queue-capacity: 100 # then CallerRunsPolicy
 
  jpa:
    hibernate:
      ddl-auto: update      # dev only — use Flyway in production
 
openai:
  service-url: http://localhost:5001
 
cors:
  allowed-origins:
    - http://localhost:3000
    - https://frontend.com
```
 
---
 
## Key Files
 
| File | Pattern | Purpose |
|---|---|---|
| `AuthFilter.java` | Servlet filter | JWT validation, SecurityContext population. Whitelists `/auth/**` and `/actuator/health`. |
| `SecurityConfig.java` | Spring Security | HttpSecurity chain. Permits public routes, adds AuthFilter, provides BCryptPasswordEncoder bean. |
| `AuthService.java` | Service | Login: BCrypt verify → JWT generate → refresh token hash + store. Refresh: lookup by hash → validate expiry → rotate. |
| `ChatService.java` | Service | BOLA check via `chat.getUser().getId()`. TX1/TX2 split. Forwards identity headers to LLMClient. |
| `ChatRepository.java` | Spring Data JPA | `findByUserId(Long, Pageable)`. `deleteByUserId(Long)` for bulk delete. |
| `LoggingFilter.java` | Servlet filter | MDC requestId. Logs method + path on start. Logs method + path + status on completion. No duration yet. |
| `GlobalExceptionHandler.java` | @ControllerAdvice | Maps exception types to HTTP status codes. Never leaks stack traces. |
| `CorsConfig.java` | Configuration | `allowCredentials(true)` for HttpOnly cookie. Exposes `X-Request-Id` response header. |
| `AsyncConfig.java` | Configuration | ThreadPoolTaskExecutor: core=10, max=50, queue=100. Configured for future `@Async` use — LLM calls in `ChatService` are currently synchronous. |
| `LLMClient.java` | HTTP client | Forwards `X-Request-Id`, `X-User-Id`, `X-User-Email`, `X-User-Role` to Flask. |
 
---
 
## Known Gaps
 
| Gap | Next Step |
|---|---|
| No registration endpoint | Add `POST /auth/register` — users currently must be seeded directly into the DB |
| No automated tests | JUnit 5 + Mockito for ChatService unit tests. `@SpringBootTest` for auth flow. |
| LoggingFilter does not measure duration | Add `startTime` capture before filter chain, log elapsed ms after |
| `ddl-auto: update` in config | Replace with Flyway migrations before production |
| No PATCH endpoint | `PUT /api/chats/{id}` handles full update; partial update not yet implemented |
 
---
 
## Running Locally
 
```bash
cd backend-service
mvn clean spring-boot:run
# Service starts on http://localhost:8080
```
 
```bash
mvn clean package
java -jar target/backend-service-0.0.1-SNAPSHOT.jar
```
