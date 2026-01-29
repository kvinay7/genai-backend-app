from flask import Blueprint, request, jsonify, g
from services.llm_service import infer_service

llm_blueprint = Blueprint("llm", __name__)

@llm_blueprint.route("/infer", methods=["POST"])
def infer_handler():
    data = request.get_json()
    if "prompt" not in data:
        return jsonify({"error": "Invalid input"}), 400

    result = infer_service(data["prompt"], g.user_id)
    return jsonify({"response": result}), 200
