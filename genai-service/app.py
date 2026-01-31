from flask import Flask
from flask_cors import CORS
from middleware.logging_middleware import log_request
from middleware.auth_middleware import authenticate
from middleware.error_handler import register_error_handlers
from handlers.llm_handler import llm_blueprint

app = Flask(__name__)
CORS(app, origins=["https://frontend.com"])

app.before_request(log_request)
app.before_request(authenticate)

register_error_handlers(app)

app.register_blueprint(llm_blueprint)

if __name__ == "__main__":
    app.run(debug=True, port=5001)
