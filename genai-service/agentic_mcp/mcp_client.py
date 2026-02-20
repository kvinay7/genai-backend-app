import requests


class MCPClient:
    def __init__(self):
        # Points to the single Flask runtime tools endpoint
        self.url = "http://localhost:5001/tools/run"

    def call_tool(self, tool_name: str, payload: dict) -> dict:
        try:
            response = requests.post(
                self.url,
                json={"tool": tool_name, "payload": payload},
                timeout=5,
            )
            response.raise_for_status()
            return response.json()
        except Exception as e:
            return {"error": str(e)}
