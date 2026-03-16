import uuid

from flask import current_app as app, g, request


def log_request():
    g.request_id = request.headers.get("X-Request-Id") or str(uuid.uuid4())
    app.logger.info("[%s] %s %s", g.request_id, request.method, request.path)
