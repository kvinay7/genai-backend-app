from typing import TypedDict, Dict, Any


class AgentState(TypedDict, total=False):
    execution_id: str
    trace_id: str
    role: str
    query: str
    intent: str
    selected_tool: str
    policy_status: str
    tool_result: Dict[str, Any]
    retry_count: int
    error: str
    response: Dict[str, Any]