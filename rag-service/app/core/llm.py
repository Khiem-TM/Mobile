from langchain_core.language_models.chat_models import BaseChatModel
from app.config import Settings


def get_llm(settings: Settings) -> BaseChatModel:
    if settings.LLM_PROVIDER == "gemini":
        from langchain_google_genai import ChatGoogleGenerativeAI
        return ChatGoogleGenerativeAI(
            model=settings.GEMINI_MODEL,
            google_api_key=settings.GEMINI_API_KEY,
            temperature=0.7,
        )

    from langchain_ollama import ChatOllama
    return ChatOllama(
        base_url=settings.OLLAMA_BASE_URL,
        model=settings.OLLAMA_LLM_MODEL,
        temperature=0.7,
    )


def get_provider_label(settings: Settings) -> str:
    if settings.LLM_PROVIDER == "gemini":
        return f"gemini/{settings.GEMINI_MODEL}"
    return f"ollama/{settings.OLLAMA_LLM_MODEL}"
