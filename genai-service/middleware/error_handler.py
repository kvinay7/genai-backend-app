from flask import jsonify
from werkzeug.exceptions import HTTPException

def register_error_handlers(app):

    @app.errorhandler(HTTPException)
    def handle_http_error(e):
        return jsonify({"error": e.description}), e.code

    @app.errorhandler(Exception)
    def handle_generic_error(e):
        app.logger.exception(e)
        return jsonify({"error": "Internal Server Error"}), 500

