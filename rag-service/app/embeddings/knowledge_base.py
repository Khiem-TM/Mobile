import json
import logging
import os
from pathlib import Path

from langchain_text_splitters import MarkdownTextSplitter

from app.core.vector_store import VectorStore

logger = logging.getLogger(__name__)

KNOWLEDGE_DIR = Path(__file__).parent.parent.parent / "knowledge"
CHUNK_SIZE = 800
CHUNK_OVERLAP = 100


def _chunk_markdown(text: str, source: str) -> list[tuple[str, dict]]:
    splitter = MarkdownTextSplitter(chunk_size=CHUNK_SIZE, chunk_overlap=CHUNK_OVERLAP)
    chunks = splitter.split_text(text)
    return [
        (chunk, {"doc_type": "knowledge", "source": source, "language": "vi"})
        for chunk in chunks
        if chunk.strip()
    ]


def _chunk_jsonl(path: Path) -> list[tuple[str, dict]]:
    results = []
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            item = json.loads(line)
            name = item.get("name", "")
            cat = item.get("category", "")
            serving = item.get("serving_g", 100)
            cal = item.get("calories", 0)
            prot = item.get("protein_g", 0)
            carbs = item.get("carbs_g", 0)
            fat = item.get("fat_g", 0)
            fiber = item.get("fiber_g", 0)
            notes = item.get("notes", "")
            text = (
                f"{name} ({cat}): {serving}g = {cal} kcal. "
                f"Đạm {prot}g, tinh bột {carbs}g, béo {fat}g, chất xơ {fiber}g. {notes}"
            )
            results.append((
                text,
                {
                    "doc_type": "vietnamese_food",
                    "source": path.name,
                    "food_name": name,
                    "category": cat,
                    "language": "vi",
                },
            ))
    return results


def embed_knowledge_base(vs: VectorStore) -> int:
    all_texts: list[str] = []
    all_ids: list[str] = []
    all_meta: list[dict] = []

    for file in sorted(KNOWLEDGE_DIR.glob("*.md")):
        content = file.read_text(encoding="utf-8")
        chunks = _chunk_markdown(content, file.name)
        for i, (text, meta) in enumerate(chunks):
            all_texts.append(text)
            all_ids.append(f"kb_{file.stem}_{i}")
            all_meta.append(meta)
        logger.info(f"Chunked {file.name}: {len(chunks)} chunks")

    for file in sorted(KNOWLEDGE_DIR.glob("*.jsonl")):
        items = _chunk_jsonl(file)
        for i, (text, meta) in enumerate(items):
            all_texts.append(text)
            all_ids.append(f"kb_{file.stem}_{i}")
            all_meta.append(meta)
        logger.info(f"Loaded {file.name}: {len(items)} food items")

    if not all_texts:
        logger.warning("No knowledge documents found in knowledge/")
        return 0

    logger.info(f"Embedding {len(all_texts)} knowledge documents...")
    vs.upsert_batch("knowledge_base", all_ids, all_texts, all_meta)
    logger.info(f"Knowledge base embedded: {len(all_texts)} documents")
    return len(all_texts)
