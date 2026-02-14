from flask import Blueprint, request, jsonify, g
from services.llm_service import LLMService

llm_blueprint = Blueprint("llm", __name__)

llm_service = LLMService()

@llm_blueprint.route("/infer", methods=["POST"])
def infer_handler():
    data = request.get_json()
    if "prompt" not in data:
        return jsonify({"error": "Invalid input"}), 400

    result = llm_service.process_query(g.user_id, data["prompt"])
    return jsonify({"response": result}), 200
