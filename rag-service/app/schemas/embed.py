from pydantic import BaseModel


class EmbedUserResponse(BaseModel):
    status: str
    user_id: str
    message: str


class EmbedKnowledgeResponse(BaseModel):
    status: str
    documents_embedded: int
