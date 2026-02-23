EMPLOYEE_DB = {
    "123": {"age": 30, "tenure": 5},
    "456": {"age": 20, "tenure": 1},
}


def _extract_employee_id(query: str) -> str:
    for token in query.split():
        if token.isdigit():
            return token
    return "123"


# --------------------------
# Eligibility Tool
# --------------------------
def eligibility_check(payload):
    query = payload.get("query", "")
    employee_id = _extract_employee_id(query)

    employee = EMPLOYEE_DB.get(employee_id)

    if not employee:
        return {
            "status": "success",
            "eligible": False,
            "reason": "Employee not found",
        }

    if employee["age"] >= 21 and employee["tenure"] >= 2:
        return {
            "status": "success",
            "eligible": True,
            "reason": "Meets criteria",
        }

    return {
        "status": "success",
        "eligible": False,
        "reason": "Does not meet criteria",
    }


# --------------------------
# Age Tool
# --------------------------
def get_employee_age(payload):
    query = payload.get("query", "")
    employee_id = _extract_employee_id(query)

    employee = EMPLOYEE_DB.get(employee_id)

    if not employee:
        return {"status": "success", "age": None}

    return {"status": "success", "age": employee["age"]}


# --------------------------
# Tool Registry
# --------------------------
TOOL_REGISTRY = {
    "eligibility_check": eligibility_check,
    "get_employee_age": get_employee_age,
}