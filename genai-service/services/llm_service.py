from repositories.chat_repository import ChatRepository
from repositories.vector_repository import search_context
from typing import Dict, Any

from agentic_mcp.graph import build_graph


class LLMService:
    def __init__(self):
        self.chat_repo = ChatRepository()
        self.graph = build_graph()

    def process_query(self, user_id: str, prompt: str) -> Dict[str, Any]:
        """Process user query using the LangGraph workflow, then save to repository."""
        initial_state = {"user_id": user_id, "query": prompt}

        result_state = self.graph.invoke(initial_state)

        response_obj = result_state.get("response")

        # create a string representation for storage
        response_text = (
            response_obj if isinstance(response_obj, str) else str(response_obj)
        )

        # Save prompt and response to repository
        chat_entry = self.chat_repo.save_chat(user_id, prompt, response_text)

        return {
            "id": chat_entry["id"],
            "user_id": user_id,
            "prompt": prompt,
            "response": response_obj,
            "created_at": chat_entry["created_at"],
        }

    def get_user_chats(self, user_id: str) -> list:
        return self.chat_repo.get_by_user_id(user_id)

    def get_chat_by_id(self, chat_id: int) -> Dict[str, Any] | None:
        return self.chat_repo.get_by_id(chat_id)

    def update_chat(self, chat_id: int, prompt: str = None, response: str = None) -> Dict[str, Any] | None:
        return self.chat_repo.update_chat(chat_id, prompt, response)

    def delete_chat(self, chat_id: int) -> bool:
        return self.chat_repo.delete_chat(chat_id)

    def delete_user_chats(self, user_id: str) -> int:
        return self.chat_repo.delete_by_user_id(user_id)


# export a simple function used by the Flask handler
_singleton = LLMService()


def infer_service(prompt: str, user_id: str):
    return _singleton.process_query(user_id, prompt)
