from flask import g, jsonify
from werkzeug.exceptions import HTTPException


def register_error_handlers(app):

    @app.errorhandler(HTTPException)
    def handle_http_error(e):
        return jsonify({
            "error": e.description,
            "requestId": getattr(g, "request_id", "n/a"),
        }), e.code

    @app.errorhandler(Exception)
    def handle_generic_error(e):
        app.logger.exception("[%s] Internal error", getattr(g, "request_id", "n/a"))
        return jsonify({
            "error": "Internal Server Error",
            "requestId": getattr(g, "request_id", "n/a"),
        }), 500
