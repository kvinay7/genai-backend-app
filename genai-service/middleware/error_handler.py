from flask import jsonify

def register_error_handlers(app):

    @app.errorhandler(Exception)
    def handle_error(e):
        app.logger.error(str(e))
        return jsonify({"error": "Internal Server Error"}), 500
