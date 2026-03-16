from flask import abort, g, request


def authenticate():
    if request.path == "/tools/run":
        return

    user_id = request.headers.get("X-User-Id")
    user_email = request.headers.get("X-User-Email")
    user_role = request.headers.get("X-User-Role", "USER")

    if not user_id or not user_email:
        abort(401, description="Missing upstream identity headers")

    g.user_id = user_id
    g.user_email = user_email
    g.user_role = user_role
