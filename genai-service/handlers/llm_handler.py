from flask import Blueprint, g, jsonify, request

from services.llm_service import LLMService

llm_blueprint = Blueprint("llm", __name__)

llm_service = LLMService()


@llm_blueprint.route("/infer", methods=["POST"])
def infer_handler():
    data = request.get_json(silent=True) or {}
    prompt = data.get("prompt")
    if not prompt:
        return jsonify({"error": "Invalid input", "requestId": g.request_id}), 400

    result = llm_service.process_query(g.user_id, g.user_role, prompt, g.request_id)
    return jsonify({"response": result, "requestId": g.request_id}), 200
