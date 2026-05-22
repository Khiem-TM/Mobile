from langchain_core.messages import AIMessage, HumanMessage, SystemMessage

from app.config import Settings
from app.core.llm import get_llm, get_provider_label
from app.core.vector_store import VectorStore
from app.rag.retriever import retrieve_context, count_sources
from app.rag.prompt_builder import build_system_prompt
from app.schemas.chat import ChatRequest, ChatResponse


async def run_rag_pipeline(request: ChatRequest, vs: VectorStore, settings: Settings) -> ChatResponse:
    # Step 1: Retrieve context from all collections
    context = await retrieve_context(vs, request.user_id, request.message)

    # Step 2: Build augmented system prompt
    system_prompt = build_system_prompt(context)

    # Step 3: Assemble messages
    messages = [SystemMessage(content=system_prompt)]
    for turn in request.conversation_history[-10:]:
        if turn.role == "user":
            messages.append(HumanMessage(content=turn.content))
        else:
            messages.append(AIMessage(content=turn.content))
    messages.append(HumanMessage(content=request.message))

    # Step 4: Call LLM
    llm = get_llm(settings)
    response = await llm.ainvoke(messages)
    reply = response.content if hasattr(response, "content") else str(response)

    return ChatResponse(
        reply=reply,
        session_id=request.session_id,
        sources_used=count_sources(context),
        provider=get_provider_label(settings),
    )
