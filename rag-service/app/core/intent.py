"""Phân loại intent bằng rule (MVP). Có thể thay bằng small-LLM ở P5.

Intent: nutrition | workout | body_metrics | mental_health | app_help | smalltalk
"""
from __future__ import annotations

import re

Intent = str

_KEYWORDS: dict[Intent, list[str]] = {
    "mental_health": [
        "stress", "căng thẳng", "lo âu", "lo lắng", "mệt mỏi", "buồn", "trầm cảm",
        "mất ngủ", "áp lực", "tâm trạng", "động lực", "chán nản", "tự ti",
    ],
    "nutrition": [
        "ăn", "calo", "calories", "dinh dưỡng", "protein", "carb", "chất béo", "đạm",
        "bữa", "thực đơn", "món", "giảm cân", "tăng cân", "khẩu phần", "nước", "uống",
    ],
    "workout": [
        "tập", "bài tập", "gym", "cardio", "chạy", "cơ", "set", "rep", "tạ",
        "lịch tập", "khởi động", "giãn cơ", "squat", "hít đất", "plank",
    ],
    "body_metrics": [
        "cân nặng", "bmi", "bmr", "tdee", "mỡ", "body fat", "vòng eo", "chỉ số",
        "tiến độ", "đo", "kg",
    ],
    "app_help": [
        "ứng dụng", "app", "tính năng", "cách dùng", "làm sao để", "ghi nhật ký",
        "đăng nhập", "thông báo", "lỗi", "sửa",
    ],
}


def classify(message: str) -> Intent:
    text = message.lower()

    def score(words: list[str]) -> int:
        return sum(1 for w in words if w in text)

    # mental_health ưu tiên cao (an toàn)
    scores = {intent: score(words) for intent, words in _KEYWORDS.items()}
    best = max(scores, key=scores.get)
    if scores[best] == 0:
        return "smalltalk"
    # Nếu có tín hiệu mental_health rõ ràng thì ưu tiên
    if scores["mental_health"] >= 1 and scores["mental_health"] >= scores[best] - 1:
        return "mental_health"
    return best


# Câu hỏi nào cần retrieval và top_k bao nhiêu (plan mục 9)
TOP_K_BY_INTENT: dict[Intent, int] = {
    "nutrition": 5,
    "workout": 5,
    "body_metrics": 3,
    "mental_health": 4,
    "app_help": 3,
    "smalltalk": 0,
}

# Intent → source filter trong vector DB
SOURCE_BY_INTENT: dict[Intent, str | None] = {
    "nutrition": "nutrition",
    "workout": "workout",
    "mental_health": "mental",
    "app_help": "faq",
    "body_metrics": None,  # ưu tiên số liệu user, ít phụ thuộc tài liệu
    "smalltalk": None,
}
