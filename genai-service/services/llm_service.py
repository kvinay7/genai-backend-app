from repositories.chat_repository import ChatRepository
from repositories.vector_repository import search_context
from typing import Dict, Any

class LLMService:
    def __init__(self):
        self.chat_repo = ChatRepository()

    def process_query(self, user_id: str, prompt: str) -> Dict[str, Any]:
        """Process user query, get LLM response, and save to repository"""
        context = search_context(prompt)
        response = f"LLM answer using: {context}"

        # Save prompt and response to repository
        chat_entry = self.chat_repo.save_chat(user_id, prompt, response)

        return {
            "id": chat_entry["id"],
            "user_id": user_id,
            "prompt": prompt,
            "response": response,
            "created_at": chat_entry["created_at"]
        }

    def get_user_chats(self, user_id: str) -> list:
        """Retrieve all chats for a user"""
        return self.chat_repo.get_by_user_id(user_id)

    def get_chat_by_id(self, chat_id: int) -> Dict[str, Any] | None:
        """Retrieve a specific chat"""
        return self.chat_repo.get_by_id(chat_id)

    def update_chat(self, chat_id: int, prompt: str = None, response: str = None) -> Dict[str, Any] | None:
        """Update a chat entry"""
        return self.chat_repo.update_chat(chat_id, prompt, response)

    def delete_chat(self, chat_id: int) -> bool:
        """Delete a chat entry"""
        return self.chat_repo.delete_chat(chat_id)

    def delete_user_chats(self, user_id: str) -> int:
        """Delete all chats for a user"""
        return self.chat_repo.delete_by_user_id(user_id)
