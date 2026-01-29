import uuid
from flask import g, request, current_app as app

def log_request():
    g.request_id = str(uuid.uuid4())
    app.logger.info(f"[{g.request_id}] {request.method} {request.path}")
