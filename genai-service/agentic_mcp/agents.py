"""Master and utility agents inside the agentic_mcp package."""
from typing import Dict

from .mcp_client import MCPClient


def master_agent(state: Dict) -> Dict:
    query = state.get("query", "")

    # Mock intent detection: look for keywords or 'employee <id>' pattern
    q = str(query).lower()
    if "eligible" in q or "eligib" in q or "employee" in q:
        state["next"] = "eligibility"
    else:
        state["response"] = "No action required."
        state["next"] = "end"

    return state


mcp = MCPClient()


def eligibility_agent(state: Dict) -> Dict:
    query = state.get("query", "")

    tool_result = mcp.call_tool("eligibility_check", {"query": query})

    state["response"] = {
        "decision": bool(tool_result.get("eligible")),
        "reason": tool_result.get("reason", "unknown"),
    }

    state["next"] = "end"
    return state
