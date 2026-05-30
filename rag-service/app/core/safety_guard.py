"""Safety guardrails (plan mục 12): pre-check input + post-check output.

MVP dùng rule/lexicon. Có thể bổ sung OpenAI moderation ở P5.
"""
from __future__ import annotations

import re
from dataclasses import dataclass


@dataclass
class Verdict:
    blocked: bool = False
    category: str | None = None
    disclaimer: str | None = None
    safe_reply: str | None = None  # nếu blocked → trả thẳng câu này


# Tín hiệu khẩn cấp về sức khỏe tâm thần / thể chất → chuyển nhánh an toàn ngay.
_CRISIS_PATTERNS = [
    r"tự (tử|sát|hại)", r"muốn chết", r"kết thúc.*cuộc đời", r"không muốn sống",
    r"đau ngực", r"khó thở", r"ngất", r"chấn thương nặng",
]

_CRISIS_REPLY = (
    "Mình rất lo cho bạn. Mình chỉ là trợ lý sức khỏe, không thể thay thế chuyên gia. "
    "Nếu bạn đang gặp nguy hiểm hoặc có ý định tự làm hại bản thân, hãy liên hệ ngay người thân, "
    "hoặc đường dây nóng hỗ trợ tâm lý / cấp cứu 115. Bạn không đơn độc, và việc tìm sự trợ giúp "
    "từ chuyên gia là điều nên làm lúc này."
)

# Lexicon dinh dưỡng nguy hiểm (post-check) — nếu output chứa, gắn cờ/disclaimer.
_UNSAFE_NUTRITION = [
    r"nhịn ăn hoàn toàn", r"bỏ (hẳn|hoàn toàn) bữa", r"chỉ (uống nước|ăn .{0,10} mỗi ngày)",
    r"giảm \d{2,}\s*kg.*(tuần|ngày)", r"thuốc giảm cân", r"detox cực đoan",
]

_MEDICAL_DISCLAIMER = (
    "Lưu ý: đây là thông tin tham khảo, không thay thế tư vấn của bác sĩ/chuyên gia dinh dưỡng."
)


def check_input(message: str, allergies: list[str] | None = None) -> Verdict:
    text = message.lower()
    for pat in _CRISIS_PATTERNS:
        if re.search(pat, text):
            return Verdict(blocked=True, category="crisis", safe_reply=_CRISIS_REPLY)
    return Verdict()


def check_output(text: str, intent: str) -> Verdict:
    low = text.lower()
    if intent in {"nutrition", "body_metrics"}:
        for pat in _UNSAFE_NUTRITION:
            if re.search(pat, low):
                return Verdict(
                    blocked=False,
                    category="unsafe_nutrition",
                    disclaimer=(
                        "Mình không khuyến nghị các biện pháp giảm cân cực đoan. "
                        "Hãy giảm cân an toàn (≤0.5–1kg/tuần) và tham khảo chuyên gia. "
                        + _MEDICAL_DISCLAIMER
                    ),
                )
    if intent == "mental_health":
        return Verdict(disclaimer="Mình ở đây để lắng nghe, nhưng không thay thế chuyên gia tâm lý.")
    if intent in {"nutrition", "workout", "body_metrics"}:
        return Verdict(disclaimer=_MEDICAL_DISCLAIMER)
    return Verdict()
