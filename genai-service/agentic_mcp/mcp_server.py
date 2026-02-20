"""MCP Blueprint inside `agentic_mcp` package."""
from flask import Blueprint, request, jsonify

from .tools import eligibility_check


mcp_blueprint = Blueprint("mcp", __name__)


@mcp_blueprint.route("/tools/run", methods=["POST"])
def run_tool():
    data = request.get_json() or {}

    tool = data.get("tool")
    payload = data.get("payload", {})

    if tool == "eligibility_check":
        result = eligibility_check(payload)
    else:
        return jsonify({"error": "Unknown tool"}), 400

    return jsonify(result), 200
