from typing import List, Dict, Any
from datetime import datetime

class ChatRepository:
    def __init__(self):
        self.store: List[Dict[str, Any]] = []
        self.id_counter = 1

    def save_chat(self, user_id: str, prompt: str, response: str) -> Dict[str, Any]:
        """Save a chat message with both prompt and response"""
        chat_entry = {
            "id": self.id_counter,
            "user_id": user_id,
            "prompt": prompt,
            "response": response,
            "created_at": datetime.now().isoformat(),
            "updated_at": datetime.now().isoformat()
        }
        self.store.append(chat_entry)
        self.id_counter += 1
        return chat_entry

    def get_by_user_id(self, user_id: str) -> List[Dict[str, Any]]:
        """Get all chats for a user"""
        return [chat for chat in self.store if chat["user_id"] == user_id]

    def get_by_id(self, chat_id: int) -> Dict[str, Any] | None:
        """Get a single chat by ID"""
        for chat in self.store:
            if chat["id"] == chat_id:
                return chat
        return None

    def update_chat(self, chat_id: int, prompt: str = None, response: str = None) -> Dict[str, Any] | None:
        """Update a chat message"""
        for chat in self.store:
            if chat["id"] == chat_id:
                if prompt:
                    chat["prompt"] = prompt
                if response:
                    chat["response"] = response
                chat["updated_at"] = datetime.now().isoformat()
                return chat
        return None

    def delete_chat(self, chat_id: int) -> bool:
        """Delete a chat by ID"""
        for i, chat in enumerate(self.store):
            if chat["id"] == chat_id:
                self.store.pop(i)
                return True
        return False

    def delete_by_user_id(self, user_id: str) -> int:
        """Delete all chats for a user, returns count deleted"""
        initial_len = len(self.store)
        self.store = [chat for chat in self.store if chat["user_id"] != user_id]
        return initial_len - len(self.store)
