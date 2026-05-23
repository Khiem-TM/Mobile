import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import get_settings
from app.core.embeddings import get_embeddings
from app.core.vector_store import init_vector_store
from app.api import health, chat, embed

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = get_settings()
    logging.getLogger().setLevel(settings.LOG_LEVEL)

    logger.info(f"Starting RAG service — LLM: {settings.LLM_PROVIDER}, Embedding: {settings.EMBEDDING_PROVIDER}")

    # Initialize vector store
    embeddings = get_embeddings(settings)
    vs = init_vector_store(settings, embeddings)
    logger.info("Vector store initialized")

    # Auto-embed knowledge base on first run
    kb_count = vs.count("knowledge_base")
    if kb_count == 0:
        logger.info("Knowledge base is empty — embedding now...")
        from app.embeddings.knowledge_base import embed_knowledge_base
        try:
            embedded = embed_knowledge_base(vs)
            logger.info(f"Knowledge base ready: {embedded} documents embedded")
        except Exception as e:
            logger.warning(f"Knowledge base embedding failed (will retry via /embed/knowledge): {e}")
    else:
        logger.info(f"Knowledge base ready: {kb_count} documents")

    yield

    logger.info("RAG service shutting down")


app = FastAPI(
    title="Calories RAG Service",
    description="RAG-powered health advisor for Calories Tracker",
    version="0.1.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(health.router)
app.include_router(chat.router)
app.include_router(embed.router)
