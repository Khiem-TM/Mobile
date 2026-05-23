BASE_SYSTEM = """Bạn là chuyên gia tư vấn sức khỏe, dinh dưỡng và thể dục cá nhân chuyên nghiệp.
Nhiệm vụ của bạn là đưa ra lời khuyên cá nhân hóa dựa trên dữ liệu thực tế của người dùng bên dưới.
Hãy trả lời bằng tiếng Việt, ngắn gọn, thực tế và dễ hiểu.
Khi đưa ra lời khuyên về dinh dưỡng, tham chiếu số liệu cụ thể từ dữ liệu người dùng.
Khi không có đủ dữ liệu, hãy nói rõ và đưa ra lời khuyên chung phù hợp."""


def build_system_prompt(context: dict[str, list[str]]) -> str:
    sections = [BASE_SYSTEM]

    if context.get("profile"):
        sections.append("\n=== HỒ SƠ SỨC KHỎE NGƯỜI DÙNG ===")
        sections.extend(context["profile"])

    if context.get("meals"):
        sections.append("\n=== LỊCH SỬ ĂN UỐNG GẦN ĐÂY ===")
        sections.extend(context["meals"])

    if context.get("workouts"):
        sections.append("\n=== LỊCH SỬ TẬP LUYỆN GẦN ĐÂY ===")
        sections.extend(context["workouts"])

    if context.get("knowledge"):
        sections.append("\n=== KIẾN THỨC DINH DƯỠNG & THỂ DỤC THAM KHẢO ===")
        sections.extend(context["knowledge"])

    return "\n".join(sections)
