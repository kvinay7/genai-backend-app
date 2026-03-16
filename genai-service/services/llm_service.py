from typing import Any, Dict
import uuid

from agentic_mcp.graph import build_graph
from repositories.chat_repository import ChatRepository


class LLMService:
    def __init__(self):
        self.chat_repo = ChatRepository()
        self.graph = build_graph()

    def process_query(self, user_id: str, role: str, prompt: str, request_id: str) -> Dict[str, Any]:
        initial_state = {
            "execution_id": str(uuid.uuid4()),
            "trace_id": request_id,
            "role": role.lower(),
            "query": prompt,
            "retry_count": 0,
        }

        result_state = self.graph.invoke(initial_state)
        response_obj = result_state.get("response")
        response_text = response_obj if isinstance(response_obj, str) else str(response_obj)

        chat_entry = self.chat_repo.save_chat(user_id, prompt, response_text)

        return {
            "id": chat_entry["id"],
            "user_id": user_id,
            "prompt": prompt,
            "response": response_obj,
            "created_at": chat_entry["created_at"],
            "trace_id": request_id,
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


_singleton = LLMService()


def infer_service(prompt: str, user_id: str):
    return _singleton.process_query(user_id, "USER", prompt, str(uuid.uuid4()))
