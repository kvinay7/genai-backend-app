"""Deterministic tools (moved into agentic_mcp package)."""

EMPLOYEE_DB = {
    "123": {"age": 30, "tenure": 5},
    "456": {"age": 20, "tenure": 1},
}


def _extract_employee_id(payload: dict) -> str:
    query = payload.get("query", "") if isinstance(payload, dict) else str(payload)
    for token in str(query).split():
        if token.isdigit():
            return token
    return "123"


def eligibility_check(payload: dict) -> dict:
    employee_id = _extract_employee_id(payload)

    employee = EMPLOYEE_DB.get(employee_id)

    if not employee:
        return {"eligible": False, "reason": "Employee not found"}

    if employee["age"] >= 21 and employee["tenure"] >= 2:
        return {"eligible": True, "reason": "Meets criteria"}
    else:
        return {"eligible": False, "reason": "Does not meet criteria"}
