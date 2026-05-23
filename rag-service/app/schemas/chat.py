from typing import Literal
from pydantic import BaseModel


class ConversationTurn(BaseModel):
    role: Literal["user", "assistant"]
    content: str


class ChatRequest(BaseModel):
    user_id: str
    session_id: str
    message: str
    conversation_history: list[ConversationTurn] = []
    stream: bool = False


class ChatResponse(BaseModel):
    reply: str
    session_id: str
    sources_used: int = 0
    provider: str
