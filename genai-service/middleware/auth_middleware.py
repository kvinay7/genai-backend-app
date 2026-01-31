from flask import request, g

def authenticate():
    # Only protect public endpoints if needed
    # Internal calls from Spring are trusted
    g.user_id = "internal-user"

