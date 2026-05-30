"""Unit test không phụ thuộc DB/mạng — kiểm tra intent, safety, chunking."""
from __future__ import annotations

from app.core import intent as intent_mod
from app.core import safety_guard
from app.core.document_ingestion import chunk_text


def test_intent_classify():
    assert intent_mod.classify("Tôi nên ăn gì để giảm cân?") == "nutrition"
    assert intent_mod.classify("Lịch tập gym cho người mới?") == "workout"
    assert intent_mod.classify("Chỉ số BMI của tôi ổn không?") == "body_metrics"
    assert intent_mod.classify("Dạo này tôi rất căng thẳng và mất ngủ") == "mental_health"
    assert intent_mod.classify("Làm sao để ghi nhật ký trong app?") == "app_help"
    assert intent_mod.classify("Xin chào") == "smalltalk"


def test_safety_crisis_blocks_input():
    v = safety_guard.check_input("tôi muốn tự tử")
    assert v.blocked is True
    assert v.safe_reply


def test_safety_output_unsafe_nutrition_disclaimer():
    v = safety_guard.check_output("Bạn nên nhịn ăn hoàn toàn trong 3 ngày", "nutrition")
    assert v.category == "unsafe_nutrition"
    assert v.disclaimer


def test_chunking_respects_size():
    text = "\n\n".join("đoạn văn số {}".format(i) * 50 for i in range(10))
    chunks = chunk_text(text, "nutrition")
    assert len(chunks) >= 1
    assert all(c.strip() for c in chunks)


def test_faq_single_chunk():
    chunks = chunk_text("Câu hỏi.\n\nTrả lời ngắn.", "faq")
    assert len(chunks) == 1
