from langgraph.graph import StateGraph, END
from .state import AgentState
from .agents import (
    router_agent,
    policy_agent,
    executor_agent,
    formatter_agent,
)


def build_graph():
    workflow = StateGraph(AgentState)

    workflow.add_node("router", router_agent)
    workflow.add_node("policy", policy_agent)
    workflow.add_node("executor", executor_agent)
    workflow.add_node("formatter", formatter_agent)

    workflow.set_entry_point("router")

    # Router Conditional
    workflow.add_conditional_edges(
        "router",
        lambda s: "error" if s.get("error") else "ok",
        {
            "error": "formatter",
            "ok": "policy",
        },
    )

    # Policy Conditional
    workflow.add_conditional_edges(
        "policy",
        lambda s: "error" if s.get("error") else "ok",
        {
            "error": "formatter",
            "ok": "executor",
        },
    )

    # Executor Retry Logic
    workflow.add_conditional_edges(
        "executor",
        lambda s: (
            "retry"
            if s.get("error") and s.get("retry_count", 0) < 2
            else "done"
        ),
        {
            "retry": "executor",
            "done": "formatter",
        },
    )

    workflow.add_edge("formatter", END)

    return workflow.compile()