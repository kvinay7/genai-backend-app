## System Requirements
- Python ≥ 3.9 (python --version)
- Java ≥ 17 (java --version)
- Maven ≥ 3.8 (mvn --version)
- Git (git --version)

## Python Setup
- cd genai-service
- python -m venv venv
- source venv/bin/activate (or) .\venv\Scripts\Activate
- pip install -r requirements.txt
- python app.py

## Java Setup
- cd backend-service
- mvn clean spring-boot:run

## API Endpoints

### 1. Create Chat Message
**POST** `/api/chats`

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

**Request Body:**
```json
{
  "userId": "user123",
  "prompt": "What is Retrieval Augmented Generation?"
}
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

---

### 2. Get All Chats for User
**GET** `/api/chats?userId=user123&page=0&size=10&sortBy=createdAt&direction=DESC`

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
**GET** `/api/chats/{id}`

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
**PUT** `/api/chats/{id}`

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

**Request Body:**
```json
{
  "prompt": "What is RAG (updated)?",
  "response": "RAG combines retrieval and generation models (updated)..."
}
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
**PATCH** `/api/chats/{id}`

**Request:**
```bash
curl -X PATCH http://localhost:8080/api/chats/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -d '{
    "response": "Updated response only..."
  }'
```

**Request Body:**
```json
{
  "response": "Updated response only..."
}
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
**DELETE** `/api/chats/{id}`

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
**DELETE** `/api/chats?userId=user123`

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

## Quick Test End-to-End Flow

```bash
# 1. Create a chat
curl -X POST http://localhost:8080/api/chats \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -d '{"userId":"user123","prompt":"What is RAG?"}'

# 2. Get all chats for user
curl -X GET "http://localhost:8080/api/chats?userId=user123" \
  -H "Authorization: Bearer test-token"

# 3. Get specific chat
curl -X GET http://localhost:8080/api/chats/1 \
  -H "Authorization: Bearer test-token"

# 4. Update chat
curl -X PUT http://localhost:8080/api/chats/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -d '{"prompt":"What is RAG?","response":"LLM answer using relevant context from vector DB"}'

# 5. Delete chat
curl -X DELETE http://localhost:8080/api/chats/1 \
  -H "Authorization: Bearer test-token"
```

