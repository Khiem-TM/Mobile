from langchain_core.embeddings import Embeddings
from app.config import Settings


class GeminiEmbeddings(Embeddings):
    """Custom embeddings using google-genai SDK v1 (supports gemini-embedding-001)."""

    def __init__(self, api_key: str, model: str = "gemini-embedding-001"):
        from google import genai
        self.client = genai.Client(api_key=api_key)
        self.model = model

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        result = self.client.models.embed_content(model=self.model, contents=texts)
        return [e.values for e in result.embeddings]

    def embed_query(self, text: str) -> list[float]:
        result = self.client.models.embed_content(model=self.model, contents=[text])
        return result.embeddings[0].values


def get_embeddings(settings: Settings) -> Embeddings:
    if settings.EMBEDDING_PROVIDER == "gemini":
        return GeminiEmbeddings(
            api_key=settings.GEMINI_API_KEY,
            model=settings.GEMINI_EMBED_MODEL,
        )

    from langchain_ollama import OllamaEmbeddings
    return OllamaEmbeddings(
        base_url=settings.OLLAMA_BASE_URL,
        model=settings.OLLAMA_EMBED_MODEL,
    )
