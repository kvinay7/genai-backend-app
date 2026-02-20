"""Graph orchestration (moved into `agentic_mcp` package)."""
from typing import Callable, Dict

END = "__END__"


class StateGraph:
    def __init__(self, state_cls=dict):
        self.state_cls = state_cls
        self.nodes: Dict[str, Callable[[dict], dict]] = {}
        self.entry_point: str | None = None
        self.conditional_edges: Dict[str, tuple[Callable, dict]] = {}
        self.edges: Dict[str, str] = {}

    def add_node(self, name: str, fn: Callable[[dict], dict]):
        self.nodes[name] = fn

    def set_entry_point(self, name: str):
        self.entry_point = name

    def add_conditional_edges(self, from_node: str, condition_fn: Callable[[dict], str], mapping: dict):
        self.conditional_edges[from_node] = (condition_fn, mapping)

    def add_edge(self, from_node: str, to_node: str):
        self.edges[from_node] = to_node

    def compile(self):
        return self

    def invoke(self, initial_state: dict) -> dict:
        if not self.entry_point:
            raise RuntimeError("No entry point set for graph")

        state = self.state_cls(initial_state)
        current = self.entry_point

        while current and current != END:
            node_fn = self.nodes.get(current)
            if not node_fn:
                break

            state = node_fn(state) or state

            if current in self.conditional_edges:
                cond_fn, mapping = self.conditional_edges[current]
                key = cond_fn(state)
                next_node = mapping.get(key)
            else:
                next_node = state.get("next")
                if not next_node:
                    next_node = self.edges.get(current)

            if not next_node or next_node == "end" or next_node == END:
                break

            current = next_node

        return state


def build_graph():
    from .agents import master_agent, eligibility_agent

    workflow = StateGraph(dict)

    workflow.add_node("master", master_agent)
    workflow.add_node("eligibility", eligibility_agent)

    workflow.set_entry_point("master")

    workflow.add_conditional_edges(
        "master",
        lambda state: state.get("next"),
        {
            "eligibility": "eligibility",
            "end": END,
        },
    )

    workflow.add_edge("eligibility", END)

    return workflow.compile()
