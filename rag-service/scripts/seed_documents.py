"""Nạp vài tài liệu kiến thức khởi đầu qua HTTP /embed/document.

Chạy khi service đã lên:  python scripts/seed_documents.py
Env: RAG_BASE_URL (default http://localhost:8001), NESTJS_INTERNAL_SECRET.
"""
from __future__ import annotations

import os

import httpx

BASE = os.environ.get("RAG_BASE_URL", "http://localhost:8001")
SECRET = os.environ.get("NESTJS_INTERNAL_SECRET", "dev-secret-change-in-prod")

DOCS: list[dict] = [
    {
        "source": "nutrition",
        "title": "Nguyên tắc giảm cân an toàn",
        "content": (
            "Giảm cân an toàn nên ở mức 0.5–1kg mỗi tuần, tương ứng thâm hụt khoảng "
            "500–700 kcal/ngày so với TDEE. Ưu tiên protein nạc (thịt gà, cá, trứng, đậu), "
            "rau xanh và carb phức (gạo lứt, yến mạch). Hạn chế đường tinh luyện và đồ chiên.\n\n"
            "Không nên nhịn ăn cực đoan hay tụt xuống dưới 1200 kcal/ngày nếu không có chỉ định "
            "của chuyên gia. Uống đủ 1.5–2.5L nước/ngày, ngủ đủ giấc để hỗ trợ trao đổi chất."
        ),
    },
    {
        "source": "nutrition",
        "title": "Cách phân bổ macro cơ bản",
        "content": (
            "Một cách phân bổ macro phổ biến cho người tập luyện: protein 1.6–2.2g/kg cân nặng, "
            "chất béo 0.8–1g/kg, phần năng lượng còn lại đến từ carbohydrate. Protein giúp giữ cơ "
            "khi giảm cân; carb cung cấp năng lượng cho buổi tập; chất béo cần cho hormone."
        ),
    },
    {
        "source": "workout",
        "title": "Khởi động và phòng chấn thương",
        "content": (
            "Luôn khởi động 5–10 phút trước khi tập: xoay khớp, cardio nhẹ, động tác mô phỏng bài "
            "chính với mức tạ nhẹ. Tăng khối lượng từ từ (nguyên tắc quá tải lũy tiến). "
            "Nếu thấy đau nhói (khác với mỏi cơ), hãy dừng lại — đừng cố tập qua cơn đau. "
            "Người mới nên tập toàn thân 3 buổi/tuần, nghỉ đủ giữa các buổi."
        ),
    },
    {
        "source": "mental",
        "title": "Quản lý căng thẳng và động lực",
        "content": (
            "Khi căng thẳng, thử bài thở 4-7-8 (hít 4 giây, giữ 7 giây, thở ra 8 giây) vài lần. "
            "Vận động nhẹ, đi bộ ngoài trời và ngủ đủ giúp cải thiện tâm trạng. Đặt mục tiêu nhỏ, "
            "khả thi để duy trì động lực. Đây là gợi ý chung, không thay thế chuyên gia tâm lý; "
            "nếu cảm giác buồn/kiệt sức kéo dài, hãy tìm sự hỗ trợ chuyên môn."
        ),
    },
    {
        "source": "faq",
        "title": "Cách ghi nhật ký bữa ăn trong app",
        "content": (
            "Để ghi bữa ăn: mở tab Nhật ký → chọn bữa (Sáng/Trưa/Tối/Phụ) → Thêm món → tìm món "
            "hoặc tạo món cá nhân → nhập khẩu phần. App tự cộng calo và macro vào tổng trong ngày, "
            "hiển thị trên Dashboard."
        ),
    },
    {
        "source": "faq",
        "title": "Cập nhật nước và số bước",
        "content": (
            "Trên màn hình chính (Home), chạm thẻ Nước để thêm 250ml mỗi lần; chạm thẻ Bước chân "
            "để nhập tổng số bước hôm nay. Số liệu đồng bộ vào thống kê hoạt động."
        ),
    },
]


def main() -> None:
    headers = {"X-Internal-Secret": SECRET}
    with httpx.Client(base_url=BASE, headers=headers, timeout=120) as client:
        for doc in DOCS:
            r = client.post("/embed/document", json=doc)
            r.raise_for_status()
            print(f"✓ {doc['title']:40s} → {r.json()}")


if __name__ == "__main__":
    main()
