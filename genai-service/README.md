# GenAI Service

Flask-based service responsible for LLM inference and Agentic MCP (Master–Utility–Tool) architecture

## Purpose
- Receive prompts from the `backend-service`
- Orchestrate agentic workflows (master → utility) via a lightweight graph
- Provide a deterministic tool boundary (MCP) that queries an in-memory DB
- Return structured JSON results to the caller

## Tech Stack
- Python 3.9+
- Flask
- Requests (for MCP client)

## Architecture
- The app runs as a single Flask process exposing:
  - `/infer` — main inference entry used by `backend-service`
  - `/tools/run` — MCP tools endpoint (registered as a blueprint)
- Agent orchestration lives in the `agentic_mcp` package:
  - `agentic_mcp/graph.py` — StateGraph + `build_graph()`
  - `agentic_mcp/agents.py` — `master_agent` and `eligibility_agent`
  - `agentic_mcp/mcp_client.py` — HTTP client for tools
  - `agentic_mcp/mcp_server.py` — Flask `mcp_blueprint` exposing tools
  - `agentic_mcp/tools.py` — deterministic tools and mock DB

### Flow
1. Flask `/infer` validates input and calls `LLMService.process_query()`
2. `LLMService` invokes the compiled `StateGraph` from `agentic_mcp.graph`
3. `master_agent` decides whether to route to `eligibility_agent`
4. `eligibility_agent` calls the MCP client → `/tools/run`
5. Tool looks up the mock DB and returns structured JSON
6. Graph finalizes the response and Flask returns it to the caller

## Quickstart (run locally)
```bash
cd genai-service
python -m venv venv
source venv/bin/activate   # Windows: venv\Scripts\activate
pip install -r requirements.txt
python app.py
```

## Quick tests
- Tools endpoint:
```bash
curl -X POST http://localhost:5001/tools/run \
  -H "Content-Type: application/json" \
  -d '{"tool":"eligibility_check","payload":{"query":"employee 123"}}'
```

- Full infer flow (master → eligibility → MCP → tool):
```bash
curl -X POST http://localhost:5001/infer \
  -H "Content-Type: application/json" \
  -d '{"prompt":"Check eligibility for employee 123"}'
```

## Notes
- The current `master_agent` uses simple keyword matching; replace with a real LLM for production.
- `agentic_mcp/tools.py` is deterministic and intentionally small so the flow is reproducible.
