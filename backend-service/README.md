# Backend Service

A production-oriented Spring Boot REST API service demonstrating enterprise-grade backend development practices, with emphasis on layered architecture, REST API design, and relational database design.

## Overview

This service provides a comprehensive REST API for chat message management in a GenAI application. It implements industry best practices for:
- **Layered Architecture** with clear separation of concerns
- **REST API Design** following HTTP standards and resource modeling
- **Database Design** with ACID transactions and optimized schemas
- **Enterprise Features** including authentication, logging, error handling, and observability

## Architecture Overview

### Layered Architecture Pattern

The application follows a strict layered architecture that promotes maintainability, testability, and scalability:

```
┌─────────────────┐
│   Controllers   │ ← HTTP Layer (REST API)
└─────────────────┘
         │
┌─────────────────┐
│    Services     │ ← Business Logic Layer
└─────────────────┘
         │
┌─────────────────┐
│  Repositories   │ ← Data Access Layer
└─────────────────┘
         │
┌─────────────────┐
│     Models      │ ← Domain Layer (JPA Entities)
└─────────────────┘
```

#### 1. Controllers Layer (HTTP/REST API)
**Purpose**: Handle HTTP requests/responses, request validation, and response formatting
- **Responsibilities**:
  - Route HTTP requests to appropriate handlers
  - Validate request bodies using Bean Validation (`@Valid`)
  - Extract authentication context from request attributes
  - Format responses with appropriate HTTP status codes
  - Handle pagination and sorting parameters

**Key Components**:
- `ChatController.java`: Main REST controller with CRUD endpoints
- Request/Response DTOs for data transfer
- Proper HTTP method usage (GET, POST, PUT, DELETE, PATCH)

#### 2. Services Layer (Business Logic)
**Purpose**: Implement business rules and orchestrate complex operations
- **Responsibilities**:
  - Coordinate between repositories and external services
  - Implement business workflows (e.g., create chat → call LLM → persist response)
  - Handle transactions for data consistency
  - Transform data between layers
  - Integrate with external services (LLM client)

**Key Components**:
- `ChatService.java`: Core business logic for chat operations
- `LLMClient.java`: Integration with GenAI service
- Transaction management with `@Transactional`

#### 3. Repositories Layer (Data Access)
**Purpose**: Abstract database operations and provide type-safe data access
- **Responsibilities**:
  - Execute database queries and updates
  - Implement custom query methods using JPA Criteria API
  - Handle pagination and sorting
  - Manage entity relationships and lazy loading

**Key Components**:
- `ChatRepository.java`: Chat message data access
- `UserRepository.java`: User data access
- `ChatSpecifications.java`: Dynamic query specifications

#### 4. Models Layer (Domain Entities)
**Purpose**: Represent database tables and business domain objects
- **Responsibilities**:
  - Define database schema through JPA annotations
  - Establish entity relationships
  - Handle data validation and constraints

**Key Components**:
- `ChatMessage.java`: Chat message entity
- `User.java`: User entity
- Proper JPA mappings and relationships

### Cross-Cutting Concerns
- **Configuration**: Centralized configuration classes
- **Security**: Authentication filters and CORS configuration
- **Exception Handling**: Global exception handler for consistent error responses
- **Logging**: Structured logging with request tracing
- **Validation**: Input validation using Bean Validation

## REST API Design

### Resource Modeling
The API follows REST principles with proper resource identification and manipulation:

```
Resource: /api/chats
├── GET    /api/chats           → List user's chats (paginated)
├── GET    /api/chats/{id}      → Get specific chat
├── POST   /api/chats           → Create new chat
├── PUT    /api/chats/{id}      → Full update chat
├── PATCH  /api/chats/{id}      → Partial update chat
└── DELETE /api/chats/{id}      → Delete specific chat
    DELETE /api/chats?userId=X  → Delete all user's chats
```

### HTTP Methods & Status Codes
- **GET**: Safe, idempotent, cacheable
- **POST**: Create resources, returns 201 Created
- **PUT**: Full updates, idempotent, returns 200 OK
- **PATCH**: Partial updates, returns 200 OK
- **DELETE**: Remove resources, returns 204 No Content

### Request/Response Patterns
- **Consistent JSON Structure**: All responses follow consistent patterns
- **Error Responses**: Structured error objects with status, message, and request ID
- **Pagination**: Spring Data Page objects for large result sets
- **Filtering**: Query parameters for user-specific data
- **Sorting**: Configurable sort fields and directions

### Authentication & Authorization
- **Bearer Token Authentication**: `Authorization: Bearer <token>` header
- **Stateless Design**: No server-side sessions
- **Request Filters**: Pre-processing authentication before controller execution

## Database Design

### Schema Overview

The database follows relational design principles with proper normalization and strategic denormalization:

```sql
-- Users table (Strong Entity)
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL
);

-- Chat messages table (Weak Entity with Foreign Key)
CREATE TABLE chat_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    prompt TEXT NOT NULL,
    response TEXT,
    user_name VARCHAR(255),  -- Denormalized for performance
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Composite index for efficient queries
CREATE INDEX idx_user_created_desc ON chat_messages(user_id, created_at DESC);
```

### Entity Relationships
- **One-to-Many**: User → ChatMessages (bidirectional)
- **Foreign Key Constraints**: Enforce referential integrity
- **Cascade Operations**: Proper cascade settings for data consistency

### Design Decisions

#### Normalization (3NF)
- **Separate Tables**: Users and chat_messages are properly separated
- **No Redundancy**: User data stored once, referenced by foreign key
- **Functional Dependencies**: All non-key attributes depend only on the primary key

#### Strategic Denormalization
- **user_name in chat_messages**: Avoids JOIN for chat history display
- **Performance Optimization**: O(1) access for frequently displayed data
- **Read-Heavy Workload**: Chat history is read much more than written

#### Indexing Strategy
- **Primary Keys**: Auto-increment BIGINT for scalability
- **Composite Index**: `(user_id, created_at DESC)` for efficient pagination
- **Foreign Key Index**: Automatic indexing on user_id

### ACID Transactions

The service demonstrates ACID principles in the chat creation workflow:

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public ChatMessage createChat(String userEmail, String prompt) {
    // Atomicity: All operations succeed or all fail
    User user = userRepository.findByEmail(userEmail)
        .orElseGet(() -> userRepository.save(new User(userEmail, "User")));
    
    ChatMessage msg = new ChatMessage(user, prompt, null);
    chatRepository.save(msg);           // Save prompt
    
    String response = llmClient.infer(prompt);  // External call
    msg.setResponse(response);
    chatRepository.save(msg);           // Save response
    
    return msg;  // Commit transaction
}
```

- **Atomicity**: "Save prompt → Call LLM → Save response" as one unit
- **Consistency**: Foreign key and NOT NULL constraints enforced
- **Isolation**: READ_COMMITTED prevents dirty reads
- **Durability**: WAL (Write-Ahead Logging) ensures persistence

## Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: H2 (development) / PostgreSQL (production)
- **ORM**: Hibernate/JPA with Spring Data JPA
- **Security**: Spring Security
- **Validation**: Jakarta Bean Validation
- **Web**: Spring Web MVC
- **Logging**: Logback with Logstash JSON encoder
- **Build**: Maven
- **Testing**: JUnit 5, Spring Boot Test

## Configuration

### Application Configuration (`application.yml`)
```yaml
cors:
  allowed-origins:
    - http://localhost:3000
    - https://frontend.com

openai:
  service-url: http://localhost:5001

spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: ""

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

  task:
    execution:
      pool:
        core-size: 10
        max-size: 50
        queue-capacity: 100
        thread-name-prefix: llm-async-
```

### Key Configuration Classes
- `CorsConfig.java`: CORS policy configuration
- `SecurityConfig.java`: Spring Security setup (CSRF disabled, stateless)
- `RestConfig.java`: REST template and web configuration
- `AsyncConfig.java`: Async thread pool for LLM calls
- `LogbackInitializer.java`: Structured logging setup

## Request Processing Flow

Every API request follows a production-grade pipeline:

1. **Client Request** → HTTP request with `Authorization: Bearer <token>`
2. **CORS Handling** → Preflight requests and origin validation
3. **Filter Chain** → LoggingFilter → AuthFilter (authentication & authorization)
4. **Controller** → Route to handler, validate input, call service
5. **Service** → Business logic, transaction management, external calls
6. **Repository** → Database operations with proper isolation
7. **Response** → Formatted JSON with appropriate HTTP status
8. **Cleanup** → MDC cleanup, response logging

## Setup & Run

### Prerequisites
- Java 17 or higher
- Maven 3.8 or higher

### Running Locally
```bash
cd backend-service
mvn clean spring-boot:run
```

### Building for Production
```bash
mvn clean package
java -jar target/backend-service-0.0.1-SNAPSHOT.jar
```

## API Endpoints

### 1. Create Chat Message
**POST** `/api/chats`
```bash
curl -X POST http://localhost:8080/api/chats \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -d '{"userId": "user123", "prompt": "What is AI?"}'
```

### 2. Get All Chats for User
**GET** `/api/chats?userId=user123&page=0&size=10&sortBy=createdAt&direction=DESC`
```bash
curl -X GET "http://localhost:8080/api/chats?userId=user123" \
  -H "Authorization: Bearer test-token"
```

### 3. Get Chat by ID
**GET** `/api/chats/{id}`
```bash
curl -X GET http://localhost:8080/api/chats/1 \
  -H "Authorization: Bearer test-token"
```

### 4. Update Chat (Full Update)
**PUT** `/api/chats/{id}`
```bash
curl -X PUT http://localhost:8080/api/chats/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -d '{"prompt": "Updated prompt", "response": "Updated response"}'
```

### 5. Partial Update Chat
**PATCH** `/api/chats/{id}`
```bash
curl -X PATCH http://localhost:8080/api/chats/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -d '{"response": "Updated response only"}'
```

### 6. Delete Chat by ID
**DELETE** `/api/chats/{id}`
```bash
curl -X DELETE http://localhost:8080/api/chats/1 \
  -H "Authorization: Bearer test-token"
```

### 7. Delete All Chats for User
**DELETE** `/api/chats?userId=user123`
```bash
curl -X DELETE "http://localhost:8080/api/chats?userId=user123" \
  -H "Authorization: Bearer test-token"
```

## Error Handling

### Global Exception Handler
The `GlobalExceptionHandler.java` provides centralized error handling:

- **400 Bad Request**: Validation errors, malformed requests
- **401 Unauthorized**: Missing or invalid authentication
- **404 Not Found**: Resource not found
- **500 Internal Server Error**: Unexpected server errors

### Error Response Format
```json
{
  "timestamp": "2026-02-09T10:30:45.123456Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "requestId": "uuid-123"
}
```

## Monitoring & Observability

### Structured Logging
- **Request Tracing**: Unique `requestId` for tracking requests
- **JSON Format**: Logstash encoder for log aggregation
- **Log Levels**: Configurable logging levels
- **Performance Metrics**: Request duration and status codes

### Health Checks
- **Spring Boot Actuator**: `/actuator/health` endpoint
- **Database Connectivity**: Automatic health checks
- **Custom Metrics**: Application-specific metrics

## Development Best Practices

### Code Organization
- **Package Structure**: Clear separation by layer
- **Dependency Injection**: Constructor injection preferred
- **SOLID Principles**: Single responsibility, open/closed, etc.
- **DRY Principle**: Avoid code duplication

### Testing Strategy
- **Unit Tests**: Service and repository layer testing
- **Integration Tests**: Full API testing with TestRestTemplate
- **Test Slices**: Focused testing with `@WebMvcTest`, `@DataJpaTest`

### Performance Considerations
- **Connection Pooling**: HikariCP for database connections
- **Async Processing**: Thread pools for external service calls
- **Pagination**: Efficient data retrieval for large datasets
- **Indexing**: Proper database indexing for query performance

This backend service demonstrates production-ready implementation of layered architecture, REST API design, and database design principles suitable for enterprise applications.

