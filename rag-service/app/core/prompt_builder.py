"""Ráp prompt (plan mục 10): system + user_context + summary + chunks + câu hỏi.

Trả về list[Message] cho LLM. Output yêu cầu Markdown thuần (NestJS/mobile render);
structured fields (intent/sources) do orchestrator điền, không bắt LLM trả JSON ở MVP
để giảm lỗi parse.
"""
from __future__ import annotations

import json
from typing import Any

from app.db.rag_repo import RetrievedChunk
from app.providers.base import Message

PROMPT_VERSION = "v1"

SYSTEM_PROMPT = """Bạn là "Vital Coach" — trợ lý sức khỏe & thể hình của ứng dụng VitalAI.
Trả lời NGẮN GỌN, thực tế, ấm áp, bằng TIẾNG VIỆT. Dùng Markdown (gạch đầu dòng khi liệt kê).

Nguyên tắc:
- Chỉ dựa trên (1) hồ sơ & số liệu người dùng được cung cấp, (2) đoạn KIẾN THỨC được truy xuất.
- KHÔNG bịa số liệu. Nếu thiếu dữ liệu, nói rõ và hỏi lại 1 câu.
- Bạn KHÔNG phải bác sĩ: không chẩn đoán bệnh, không kê thuốc, không khuyên nhịn ăn cực đoan
  hay giảm cân quá 1kg/tuần.
- LUÔN tôn trọng dị ứng thực phẩm của người dùng (không gợi ý món chứa thành phần họ dị ứng).
- Với dấu hiệu nghiêm trọng (tự hại, đau ngực, chấn thương) → khuyên dừng lại và tìm chuyên gia/y tế.
"""

_INTENT_GUIDE: dict[str, str] = {
    "nutrition": "Tập trung tư vấn dinh dưỡng: bám mục tiêu calo/macro và dị ứng của người dùng. "
                 "Gợi ý cụ thể, ước lượng calo khi có thể.",
    "workout": "Tập trung tư vấn tập luyện: bám mục tiêu, mức độ và lưu ý an toàn/chấn thương. "
               "Đề xuất bài tập/khối lượng hợp lý.",
    "body_metrics": "Phân tích xu hướng chỉ số cơ thể từ số liệu được cung cấp (cân nặng, BMI, TDEE...). "
                    "Nhận xét tiến độ so với mục tiêu, KHÔNG phán đoán bệnh lý.",
    "mental_health": "Lắng nghe, đồng cảm, đưa gợi ý nhẹ nhàng (thở, nghỉ ngơi, vận động nhẹ). "
                     "Không chẩn đoán; khuyến khích tìm chuyên gia khi cần.",
    "app_help": "Hướng dẫn sử dụng ứng dụng dựa trên phần FAQ được cung cấp. Trả lời theo bước.",
    "smalltalk": "Trò chuyện thân thiện, ngắn gọn, và kéo người dùng về chủ đề sức khỏe/thể hình.",
}


def _format_context(user_context: dict[str, Any] | None) -> str:
    if not user_context:
        return "(Không có dữ liệu người dùng — hãy hỏi thêm nếu cần.)"
    return json.dumps(user_context, ensure_ascii=False, indent=2)


def _format_chunks(chunks: list[RetrievedChunk]) -> str:
    if not chunks:
        return "(Không có đoạn kiến thức liên quan.)"
    lines = []
    for i, c in enumerate(chunks, 1):
        lines.append(f"[Nguồn {i}: {c.title}]\n{c.content}")
    return "\n\n".join(lines)


def build_messages(
    *,
    intent: str,
    message: str,
    user_context: dict[str, Any] | None,
    recent_summary: str | None,
    chunks: list[RetrievedChunk],
    history: list[Message] | None = None,
) -> list[Message]:
    guide = _INTENT_GUIDE.get(intent, _INTENT_GUIDE["smalltalk"])
    system = SYSTEM_PROMPT + f"\n\nNgữ cảnh câu hỏi (intent = {intent}): {guide}"

    context_block = (
        f"### DỮ LIỆU NGƯỜI DÙNG\n{_format_context(user_context)}\n\n"
        + (f"### TÓM TẮT GẦN ĐÂY\n{recent_summary}\n\n" if recent_summary else "")
        + f"### KIẾN THỨC TRUY XUẤT\n{_format_chunks(chunks)}"
    )

    messages: list[Message] = [Message(role="system", content=system)]
    messages.append(Message(role="system", content=context_block))
    if history:
        messages.extend(history[-6:])  # giữ tối đa 6 lượt gần nhất
    messages.append(Message(role="user", content=message))
    return messages
