"""Pipeline nạp tài liệu: clean → chunk → embed → index (plan mục 7-8).

Chunking MVP: cắt theo đoạn rồi gộp đến ~max_tokens (xấp xỉ bằng số từ), overlap nhẹ.
FAQ: mỗi cặp Q&A là 1 chunk (truyền sẵn từng đoạn ngắn).
"""
from __future__ import annotations

import re

import structlog

from app.core.embedding_service import EmbeddingService
from app.db.rag_repo import RagRepo

log = structlog.get_logger(__name__)

# kích thước xấp xỉ theo "từ" (đơn giản cho MVP; thay bằng tokenizer thật ở P5)
_CHUNK_PARAMS = {
    "nutrition": (500, 70),
    "workout": (400, 40),
    "mental": (400, 60),
    "faq": (10_000, 0),  # 1 chunk / tài liệu FAQ ngắn
}


def chunk_text(text: str, source: str) -> list[str]:
    max_words, overlap = _CHUNK_PARAMS.get(source, (500, 60))
    text = re.sub(r"\r\n", "\n", text).strip()
    paragraphs = [p.strip() for p in re.split(r"\n\s*\n", text) if p.strip()]

    chunks: list[str] = []
    buf: list[str] = []
    count = 0
    for para in paragraphs:
        words = para.split()
        if count + len(words) > max_words and buf:
            chunks.append(" ".join(buf))
            # overlap: giữ lại `overlap` từ cuối
            tail = " ".join(buf).split()[-overlap:] if overlap else []
            buf = tail.copy()
            count = len(tail)
        buf.extend(words)
        count += len(words)
    if buf:
        chunks.append(" ".join(buf))
    return chunks or [text]


async def ingest_document(
    repo: RagRepo,
    embedder: EmbeddingService,
    *,
    source: str,
    title: str,
    content: str,
    lang: str = "vi",
    quality: str = "curated",
    extra_metadata: dict | None = None,
) -> tuple[str, int]:
    doc = await repo.create_document(source=source, title=title, lang=lang, quality=quality)
    pieces = chunk_text(content, source)
    embeddings = await embedder.embed_documents(pieces)
    meta_base = {"source": source, "title": title, "version": 1, "quality": quality, "lang": lang}
    if extra_metadata:
        meta_base.update(extra_metadata)
    rows = [(piece, emb, dict(meta_base, chunk_index=i)) for i, (piece, emb) in enumerate(zip(pieces, embeddings))]
    n = await repo.add_chunks(doc.id, rows)
    log.info("ingested_document", title=title, source=source, chunks=n)
    return str(doc.id), n
