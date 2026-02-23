import os
from dotenv import load_dotenv

load_dotenv()

OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")

class LLMClient:
    def __init__(self, model="gpt-3.5-turbo", temperature=0.7, api_key=OPENAI_API_KEY):
        self.model = model
        self.temperature = temperature
        self.api_key = api_key

    def chat(self, messages):
        pass  # Implement actual API call to OpenAI here, returning the response text