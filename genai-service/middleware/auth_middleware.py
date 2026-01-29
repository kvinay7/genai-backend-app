from flask import request, jsonify, g

def authenticate():
    token = request.headers.get("Authorization")
    if not token:
        return jsonify({"error": "Unauthorized"}), 401

    g.user_id = "user-123"
