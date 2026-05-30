"""Prometheus metrics (plan mục 16)."""
from __future__ import annotations

from prometheus_client import Counter, Histogram

CHAT_REQUESTS = Counter("rag_chat_requests_total", "Số request chat", ["intent", "status"])
CHAT_LATENCY = Histogram("rag_chat_latency_seconds", "Độ trễ xử lý chat", ["intent"])
LLM_TOKENS = Counter("rag_llm_tokens_total", "Token LLM", ["model", "direction"])
LLM_COST = Counter("rag_llm_cost_usd_total", "Chi phí LLM (USD)", ["model"])
RETRIEVAL_HITS = Histogram("rag_retrieval_hits", "Số chunk truy xuất", ["intent"])
SAFETY_EVENTS = Counter("rag_safety_events_total", "Sự kiện safety", ["result"])
