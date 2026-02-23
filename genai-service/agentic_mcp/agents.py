import time
import re
import json
from typing import Dict

from llm_config import LLMClient
from .mcp_client import MCPClient

llm = LLMClient()
mcp = MCPClient()


# ==========================================================
# 1️⃣ LLM Router Agent (Structured + Safe + Production Ready)
# ==========================================================
def router_agent(state: Dict) -> Dict:
    query = state.get("query", "")

    system_prompt = """
You are a routing agent.

Respond ONLY in valid JSON format like:

{
  "tool": "eligibility_check"
}

Available tools:
- eligibility_check
- get_employee_age

Do not add explanations.
"""

    response = {"tool": "eligibility_check"} # default response for testing

    if not response:
        state["error"] = "LLM returned empty response"
        return state

    # cleaned = re.sub(r"```json|```", "", response).strip()

    try:
        parsed = response
        tool = parsed.get("tool")
    except Exception:
        state["error"] = "Invalid JSON returned by LLM"
        return state

    if tool not in ["eligibility_check", "get_employee_age"]:
        state["error"] = "Unsupported tool selected"
        return state

    state["intent"] = tool
    state["selected_tool"] = tool
    return state


# ==========================================================
# 2️⃣ Policy Agent (Role-based access control)
# ==========================================================
def policy_agent(state: Dict) -> Dict:
    role = state.get("role", "user")

    if role not in ["admin", "hr"]:
        state["error"] = "Unauthorized access"

    return state


# ==========================================================
# 3️⃣ Executor Agent (Dynamic tool execution + retry)
# ==========================================================
def executor_agent(state: dict) -> dict:
    if state.get("error") and state.get("retry_count", 0) >= 2:
        return state

    tool = state.get("selected_tool")

    if not tool:
        state["error"] = "No tool selected"
        return state

    start = time.time()
    result = mcp.call_tool(tool, {"query": state.get("query")})
    latency = int((time.time() - start) * 1000)

    if "error" in result:
        state["retry_count"] = state.get("retry_count", 0) + 1
        state["error"] = result["error"]
    else:
        state.pop("error", None)
        state["tool_result"] = result
        state["latency_ms"] = latency

    return state


# ==========================================================
# 4️⃣ Formatter Agent (Structured production response)
# ==========================================================
def formatter_agent(state: Dict) -> Dict:
    execution_id = state.get("execution_id")
    trace_id = state.get("trace_id")

    if state.get("error"):
        state["response"] = {
            "execution_id": execution_id,
            "status": "failed",
            "error": state["error"],
            "retry_count": state.get("retry_count", 0),
            "trace_id": trace_id,
        }
        return state

    state["response"] = {
        "execution_id": execution_id,
        "status": "success",
        "data": state.get("tool_result"),
        "metadata": {
            "intent": state.get("intent"),
            "role": state.get("role"),
            "latency_ms": state.get("latency_ms"),
            "retry_count": state.get("retry_count", 0),
            "trace_id": trace_id,
        },
    }

    return state