# GenAI Service

Flask microservice for LLM inference, LangGraph agentic workflows, MCP tool execution, and RAG pipeline. Spring Boot is the authentication boundary - this service consumes forwarded identity, it does not validate JWTs.

---

## What This Service Owns

| Responsibility | Implementation |
|---|---|
| Identity consumption | Reads `X-User-Id`, `X-User-Email`, `X-User-Role` from Spring Boot; defaults role to `USER` if absent |
| Request tracing | Reuses `X-Request-Id` from upstream if present; generates fallback UUID if absent |
| Agentic workflow | LangGraph StateGraph: Router -> Policy -> Executor -> Formatter |
| Tool execution | MCP server exposing deterministic tool registry |
| RAG pipeline | PDF loading, chunking, embedding, vector retrieval |
| Chat persistence | In-memory chat repository (upgrade path: PostgreSQL) |

---

## Architecture

```text
POST /infer  (called by Spring Boot LLMClient)
  |
  |-- logging_middleware    reuses X-Request-Id if present; else generates UUID
  |-- auth_middleware       reads X-User-Id/Email/Role; defaults role to USER if absent
  |-- llm_handler.py        validates prompt, calls LLMService
  |-- LLMService            builds AgentState, invokes compiled graph
  |
  `-- LangGraph StateGraph
        |-- Router Agent     -> selects tool based on query
        |-- Policy Agent     -> checks role: admin/hr pass, others -> error path
        |-- Executor Agent   -> calls MCPClient, max 2 retries on failure
        `-- Formatter Agent  -> builds structured response with metadata
              |
              v
        POST /tools/run (MCP Server)
              |
              v
        TOOL_REGISTRY[tool_name](payload)   // deterministic Python function
```

---

## Middleware - Actual Behavior

### `logging_middleware.py`

```python
# Reuses X-Request-Id from upstream header.
# If the header is absent (e.g. direct call bypassing Spring), generates a new UUID.
request_id = request.headers.get("X-Request-Id") or str(uuid.uuid4())
g.request_id = request_id
```

The fallback UUID generation means trace continuity is preserved when called from Spring, but a direct call to Flask gets its own isolated trace ID.

### `auth_middleware.py`

```python
# auth_middleware.py - actual behavior
user_id = request.headers.get("X-User-Id")
user_email = request.headers.get("X-User-Email")
user_role = request.headers.get("X-User-Role", "USER")

if not user_id or not user_email:
    abort(401, description="Missing upstream identity headers")

g.user_id = user_id
g.user_email = user_email
g.user_role = user_role
```

`X-User-Id` and `X-User-Email` are required - missing either aborts with `401`. `X-User-Role` is optional - defaults to `USER`, which the Policy agent will reject for tool operations requiring `admin` or `hr`.

### `error_handler.py`

```python
# Error responses include requestId but not a timestamp field.
return jsonify({
    "error": "Internal Server Error",
    "requestId": getattr(g, "request_id", "n/a"),
}), 500
```

---

## Why Flask is Not the Auth Boundary

This service is internal - it only receives calls from Spring Boot's `LLMClient`. Spring has already validated the JWT before forwarding the request. Flask trusts the forwarded identity headers.

**Why not validate JWT in Flask too?**
Double validation adds latency and duplicates auth logic. In a microservice architecture, the edge service (Spring Boot) authenticates. Internal services consume the verified identity. Flask should never be directly accessible by clients - only by Spring Boot.

---

## LangGraph State Machine

### AgentState

```python
class AgentState(TypedDict):
    execution_id:  str             # UUID for this workflow run
    trace_id:      str             # X-Request-Id from Spring - ties to Spring logs
    role:          str             # from X-User-Role header (default: USER)
    query:         str             # user prompt
    intent:        Optional[str]   # tool name selected by Router
    selected_tool: Optional[str]   # confirmed tool for Executor
    tool_result:   Optional[Dict]  # result from MCP tool
    retry_count:   int             # Executor retry counter
    error:         Optional[str]   # error message if any agent failed
    response:      Optional[Dict]  # final formatted response
    latency_ms:    Optional[int]   # total workflow execution time
```

### Graph Definition

```python
# graph.py
graph = StateGraph(AgentState)

graph.add_node("router", router_agent)
graph.add_node("policy", policy_agent)
graph.add_node("executor", executor_agent)
graph.add_node("formatter", formatter_agent)

graph.set_entry_point("router")

graph.add_conditional_edges(
    "router",
    lambda s: "error" if s.get("error") else "ok",
    {
        "error": "formatter",
        "ok": "policy",
    },
)

graph.add_conditional_edges(
    "policy",
    lambda s: "error" if s.get("error") else "ok",
    {
        "error": "formatter",
        "ok": "executor",
    },
)

graph.add_conditional_edges(
    "executor",
    lambda s: "retry" if s.get("error") and s.get("retry_count", 0) < 2 else "done",
    {
        "retry": "executor",
        "done": "formatter",
    },
)

graph.add_edge("formatter", END)
compiled = graph.compile()
```

### Why LangGraph over a chain?

A LangChain chain is linear: `prompt | llm | parser`. The workflow here requires:
- **Branching**: policy failure skips executor entirely
- **Retry loops**: executor retries up to 2 times on tool failure
- **State accumulation**: each node reads and updates shared typed state

A graph expresses all of this explicitly. The control flow is visible, testable, and debuggable.

---

## Agent Implementations

### Router Agent
**Input:** `query` from state  
**Output:** sets `intent` and `selected_tool`  
**Current:** deterministic string matching  
**Next step:** LLM-based routing - prompt with `query + tool_list`, parse JSON response with Pydantic `RouterDecision(tool: str, confidence: float)`

### Policy Agent
**Input:** `role` from state  
**Output:** passes or sets `error` in state

```python
AUTHORIZED_ROLES = {"admin", "hr"}

def policy_agent(state: AgentState) -> AgentState:
    if state["role"] not in AUTHORIZED_ROLES:
        return {**state, "error": "Unauthorized access"}
    return state
```

Default role is `USER` (set by auth_middleware when header is absent). `USER` is not in `AUTHORIZED_ROLES` - so any request without an explicit admin/hr role will be rejected at the Policy node.

### Executor Agent
**Input:** `selected_tool`, `query`, `retry_count`  
**Output:** sets `tool_result` or increments `retry_count` + sets `error`

```python
def executor_agent(state: dict) -> dict:
    if state.get("error") and state.get("retry_count", 0) >= 2:
        return state

    tool = state.get("selected_tool")
    if not tool:
        state["error"] = "No tool selected"
        return state

    result = mcp.call_tool(tool, {"query": state.get("query")})
    if "error" in result:
        state["retry_count"] = state.get("retry_count", 0) + 1
        state["error"] = result["error"]
    else:
        state.pop("error", None)
        state["tool_result"] = result

    return state
```

### Formatter Agent
**Input:** entire final state  
**Output:** structured JSON response with metadata

```python
{
    "execution_id": state["execution_id"],
    "trace_id": state["trace_id"],
    "status": "success" or "failed",
    "data": state.get("tool_result"),
    "metadata": {
        "intent": state.get("intent"),
        "role": state["role"],
        "latency_ms": state.get("latency_ms"),
        "retry_count": state["retry_count"]
    }
}
```

`trace_id` in the response lets the caller correlate this workflow run with Spring Boot logs using a single ID.

---

## MCP - Model Context Protocol

### What Problem MCP Solves

Without MCP, an LLM deciding which function to call could be manipulated by prompt injection. With MCP, the LLM can only select from named tools in `TOOL_REGISTRY`. Undeclared functions cannot be reached.

```text
LLM decision  ->  tool name (string)
     |
     v
MCPClient.run_tool(tool_name, payload)
     |
     v
POST /tools/run  {tool: "eligibility_check", payload: {...}}
     |
     v
MCPServer looks up TOOL_REGISTRY[tool_name]
     |
     v
Deterministic Python function executes
     |
     v
JSON result returned
```

```python
# tools.py
TOOL_REGISTRY = {
    "eligibility_check": check_eligibility,
    "get_employee_age": get_employee_age,
}

EMPLOYEE_DB = {
    "123": {"age": 30, "tenure": 5},
    "456": {"age": 20, "tenure": 1},
}
```

---

## RAG Pipeline

```python
# rag_pipeline.py
class RAGPipeline:
    def process_document(self, pdf_path: str):
        text = self._extract_pdf(pdf_path)
        chunks = self._chunk_text(text, chunk_size=800, overlap=150)
        embeddings = self._embed(chunks)
        self.vector_repo.store(chunks, embeddings)

    def retrieve(self, query: str, k: int = 5) -> list[str]:
        query_embedding = self._embed([query])[0]
        return self.vector_repo.search(query_embedding, k=k)
```

**Why overlap=150?** Without overlap, a sentence split across two chunks loses context at the boundary. Overlap helps preserve continuity.

**Current vector store:** FAISS (in-memory). Does not persist across restarts.  
**Upgrade path:** pgvector - `vector(1536)` column in PostgreSQL, HNSW index for cosine similarity.

---

## API Endpoints

### `POST /infer`

```bash
curl -X POST http://localhost:5001/infer \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: uuid-from-spring" \
  -H "X-User-Id: 42" \
  -H "X-User-Email: user@example.com" \
  -H "X-User-Role: admin" \
  -d '{"prompt": "Check eligibility for employee 123"}'
```

Response:

```json
{
  "requestId": "uuid-from-spring",
  "response": {
    "id": 1,
    "user_id": "42",
    "prompt": "Check eligibility for employee 123",
    "response": {
      "execution_id": "uuid",
      "status": "success",
      "data": {
        "status": "success",
        "eligible": true,
        "reason": "Meets criteria"
      },
      "metadata": {
        "intent": "eligibility_check",
        "role": "admin",
        "latency_ms": 45,
        "retry_count": 0,
        "trace_id": "uuid-from-spring"
      }
    },
    "created_at": "2026-03-16T16:00:00",
    "trace_id": "uuid-from-spring"
  }
}
```

### `POST /tools/run`

```bash
curl -X POST http://localhost:5001/tools/run \
  -H "Content-Type: application/json" \
  -d '{"tool": "eligibility_check", "payload": {"query": "employee 123"}}'
```

Response:

```json
{
  "status": "success",
  "eligible": true,
  "reason": "Meets criteria"
}
```

---

## File Guide

| File | What it does | Key interview point |
|---|---|---|
| `state.py` | AgentState TypedDict | Shared typed state object. `trace_id` = upstream requestId. `role` defaults to USER if header absent. |
| `graph.py` | StateGraph definition | `add_node`, `add_conditional_edges`, `compile()`. Explicit control flow instead of implicit callbacks. |
| `agents.py` | 4 agent functions | Pure functions: state in, updated state out. Policy checks role. Executor retries up to 2 times. |
| `mcp_client.py` | HTTP client for tools | Sends POST `/tools/run`. Decouples executor logic from tool implementation. |
| `mcp_server.py` | Flask blueprint | Receives `/tools/run`, looks up `TOOL_REGISTRY`, executes function. |
| `tools.py` | Tool implementations | Deterministic Python. LLM selects tool name - it does not run the code. |
| `llm_service.py` | Workflow orchestration | Builds AgentState with trace_id + role from `g` context. Invokes compiled graph. Persists chat. |
| `auth_middleware.py` | Identity injection | Reads `X-User-*` headers into `g.user_id`, `g.user_email`, `g.user_role`. Aborts 401 if user_id or user_email missing. |
| `logging_middleware.py` | Trace ID handling | Reuses `X-Request-Id` if present. Generates fallback UUID if absent. |
| `error_handler.py` | Structured errors | All errors include requestId. No timestamp field currently. |
| `rag_pipeline.py` | Document processing | PDF -> pdfplumber -> chunk -> embed -> FAISS store -> cosine retrieval. |

---

## Known Gaps

| Gap | Next Step |
|---|---|
| LangGraph router is deterministic | Replace with LLM call + structured router output |
| FAISS is in-memory | Replace with pgvector + HNSW index |
| No RAGAS evaluation | Add evaluation script for faithfulness/relevancy/context precision |
| No LangSmith tracing | Add `LANGCHAIN_TRACING_V2`, API key, and project config |
| No prompt injection guard | Add regex/blocklist or prompt-hardening layer before routing |
| No PII masking | Add masking before model/tool calls |
| `error_handler.py` has no timestamp | Add `"timestamp": datetime.utcnow().isoformat()` |

---

## Running Locally

```bash
cd genai-service
python -m venv venv
source venv/bin/activate   # Windows: venv\Scripts\activate
pip install -r requirements.txt
echo "OPENAI_API_KEY=your_key_here" > .env
python app.py
```

```bash
# Test tool directly
curl -X POST http://localhost:5001/tools/run \
  -H "Content-Type: application/json" \
  -d '{"tool": "eligibility_check", "payload": {"query": "employee 123"}}'
```
