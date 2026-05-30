"""Đọc READ-ONLY dữ liệu nghiệp vụ (schema public) qua user `rag_reader`.

Dùng raw SQL theo schema thật của NestJS (xem plan mục 2) để tránh phụ thuộc
vào TypeORM entity. KHÔNG ghi vào business tables.
"""
from __future__ import annotations

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession


class BusinessRepo:
    def __init__(self, session: AsyncSession) -> None:
        self.s = session

    async def get_exercises_for_embedding(self, limit: int = 1000) -> list[dict]:
        """Lấy thư viện bài tập để embedding phần mô tả (knowledge)."""
        rows = (
            await self.s.execute(
                text(
                    """
                    SELECT id, name, description, instructions, form_tips,
                           muscle_group, difficulty_level, met_value, exercise_type
                    FROM exercises
                    WHERE is_active = true
                      AND (description IS NOT NULL OR instructions IS NOT NULL)
                    LIMIT :limit
                    """
                ),
                {"limit": limit},
            )
        ).mappings().all()
        return [dict(r) for r in rows]

    async def lookup_food(self, name: str, limit: int = 5) -> list[dict]:
        """Tra cứu nhanh food theo tên (structured, KHÔNG embedding)."""
        rows = (
            await self.s.execute(
                text(
                    """
                    SELECT name, calories_per_100g, protein_per_100g, fat_per_100g,
                           carbs_per_100g, serving_size_g, serving_unit
                    FROM foods
                    WHERE is_active = true AND name ILIKE :q
                    ORDER BY favorites_count DESC NULLS LAST
                    LIMIT :limit
                    """
                ),
                {"q": f"%{name}%", "limit": limit},
            )
        ).mappings().all()
        return [dict(r) for r in rows]
