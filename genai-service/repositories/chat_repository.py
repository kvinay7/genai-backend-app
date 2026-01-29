store = []

def save_chat(user_id, message):
    store.append({
        "user_id": user_id,
        "message": message
    })
