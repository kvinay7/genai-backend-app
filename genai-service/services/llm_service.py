from repositories.chat_repository import save_chat
from repositories.vector_repository import search_context

def infer_service(prompt, user_id):
    context = search_context(prompt)
    response = f"LLM answer using: {context}"

    save_chat(user_id, prompt)
    save_chat(user_id, response)

    return response
