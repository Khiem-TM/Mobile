"""Bộ nhớ hội thoại theo user (rolling summary). MVP: đọc summary có sẵn.

Việc cập nhật summary (tóm tắt N lượt) sẽ hoàn thiện ở Phase 2; ở đây cung cấp
sẵn API để orchestrator gọi mà không cần đổi interface sau này.
"""
from __future__ import annotations

from app.db.rag_repo import RagRepo


class UserMemory:
    def __init__(self, repo: RagRepo) -> None:
        self.repo = repo

    async def get_summary(self, user_ref: str) -> str | None:
        mem = await self.repo.get_memory(user_ref)
        return mem.summary if mem else None

    async def update(self, user_ref: str, summary: str, facts: list | None = None) -> None:
        await self.repo.upsert_memory(user_ref, summary, facts)
