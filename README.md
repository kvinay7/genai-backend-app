## System Requirements
- Python ≥ 3.9 (python --version)
- Java ≥ 17 (java --version)
- Maven ≥ 3.8 (mvn --version)
- Git (git --version)

## Python Setup
- cd genai-service
- python -m venv venv
- source venv/bin/activate
- pip install -r requirements.txt
- python app.py

## Java Setup
- cd backend-service
- mvn clean spring-boot:run

## TEST END-TO-END FLOW

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -d '{"prompt":"What is RAG?"}'
````

```json
{
  "response": "LLM answer using: Relevant context from vector DB"
}
```

