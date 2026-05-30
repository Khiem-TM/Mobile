"""Sinh tóm tắt ngày / báo cáo tuần và cập nhật bộ nhớ hội thoại (P2).

Dùng LLM để cô đọng dữ liệu (đã do NestJS cung cấp ở dạng UserContext) thành
văn bản tiếng Việt ngắn gọn, động viên, an toàn.
"""
from __future__ import annotations

import json
from datetime import date as date_cls
from typing import Any

import structlog

from app.core.llm_service import LLMService
from app.db.rag_repo import RagRepo
from app.providers.base import Message

log = structlog.get_logger(__name__)

# Số lượt hội thoại giữa 2 lần làm mới summary (giảm chi phí LLM).
_MEMORY_REFRESH_EVERY = 6


class SummaryService:
    def __init__(self, repo: RagRepo, llm: LLMService) -> None:
        self.repo = repo
        self.llm = llm

    @staticmethod
    def _parse_date(s: str) -> date_cls:
        return date_cls.fromisoformat(s)

    # ── Daily summary ───────────────────────────────────────────────────────
    async def generate_daily(self, user_ref: str, date: str, user_context: dict | None) -> tuple[str, dict]:
        ctx_json = json.dumps(user_context or {}, ensure_ascii=False)
        prompt = (
            "Dựa trên dữ liệu sức khỏe trong ngày của người dùng (JSON dưới đây), viết một đoạn "
            "TÓM TẮT NGÀY ngắn gọn (3-4 câu) bằng tiếng Việt: nhận xét lượng calo nạp vào so với "
            "mục tiêu, vận động/bước chân, nước uống, và 1 gợi ý nhẹ nhàng cho ngày mai. "
            "Giọng tích cực, KHÔNG phán đoán bệnh, KHÔNG khuyên cực đoan.\n\n"
            f"DỮ LIỆU:\n{ctx_json}"
        )
        result = await self.llm.generate([Message(role="user", content=prompt)])
        metrics = (user_context or {}).get("today", {}) if user_context else {}
        await self.repo.upsert_daily_summary(user_ref, self._parse_date(date), result.text, metrics)
        log.info("daily_summary_generated", user_ref=user_ref, date=date, model=result.model)
        return result.text, metrics

    # ── Weekly report ───────────────────────────────────────────────────────
    async def generate_weekly(self, user_ref: str, week_start: str, user_context: dict | None) -> dict:
        ctx_json = json.dumps(user_context or {}, ensure_ascii=False)
        prompt = (
            "Dựa trên dữ liệu 7 ngày của người dùng (JSON), viết BÁO CÁO TUẦN tiếng Việt gồm: "
            "1) Nhận xét tổng quan, 2) Điểm tốt, 3) Điểm cần cải thiện, 4) 2-3 gợi ý cho tuần tới. "
            "Ngắn gọn, tích cực, an toàn (không chẩn đoán bệnh, không khuyên giảm cân cực đoan).\n\n"
            f"DỮ LIỆU:\n{ctx_json}"
        )
        result = await self.llm.generate([Message(role="user", content=prompt)])
        report = {"narrative": result.text, "trend": (user_context or {}).get("trend_7d", {})}
        await self.repo.upsert_weekly_report(user_ref, self._parse_date(week_start), report)
        log.info("weekly_report_generated", user_ref=user_ref, week_start=week_start, model=result.model)
        return report

    # ── Rolling conversation memory ─────────────────────────────────────────
    def should_refresh(self, history_len: int) -> bool:
        # làm mới ở lượt đầu và mỗi _MEMORY_REFRESH_EVERY lượt
        return history_len == 0 or (history_len % _MEMORY_REFRESH_EVERY == 0)

    async def refresh_conversation_memory(
        self, user_ref: str, prev_summary: str | None, turns: list[Message], latest_user: str, latest_assistant: str
    ) -> None:
        convo = "\n".join(f"{m.role}: {m.content}" for m in turns[-8:])
        convo += f"\nuser: {latest_user}\nassistant: {latest_assistant}"
        prompt = (
            "Cập nhật BẢN GHI NHỚ ngắn (tối đa 100 từ, tiếng Việt) về người dùng dựa trên tóm tắt cũ "
            "và đoạn hội thoại mới. Chỉ giữ thông tin bền vững & hữu ích để cá nhân hoá tư vấn "
            "(mục tiêu, sở thích, ràng buộc, chủ đề lặp lại). KHÔNG chứa thông tin định danh cá nhân.\n\n"
            f"TÓM TẮT CŨ:\n{prev_summary or '(chưa có)'}\n\nHỘI THOẠI MỚI:\n{convo}\n\nBẢN GHI NHỚ MỚI:"
        )
        try:
            result = await self.llm.generate([Message(role="user", content=prompt)])
            await self.repo.upsert_memory(user_ref, result.text.strip())
        except Exception as e:  # best-effort, không chặn UX
            log.warning("memory_refresh_failed", err=str(e))
