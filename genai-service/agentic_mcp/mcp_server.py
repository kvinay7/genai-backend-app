from flask import Blueprint, request, jsonify
from agentic_mcp.tools import TOOL_REGISTRY

mcp_blueprint = Blueprint("mcp", __name__)


@mcp_blueprint.route("/tools/run", methods=["POST"])
def run_tool():
    data = request.get_json() or {}

    tool_name = data.get("tool")
    payload = data.get("payload", {})

    if tool_name not in TOOL_REGISTRY:
        return jsonify({"error": "Unknown tool"}), 400

    try:
        result = TOOL_REGISTRY[tool_name](payload)
        return jsonify(result), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500