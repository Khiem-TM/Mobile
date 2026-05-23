"""Convert structured DB rows to Vietnamese natural language for embedding."""
from datetime import date


_GENDER_VI = {"male": "nam", "female": "nữ", "other": "khác"}
_ACTIVITY_VI = {
    "sedentary": "ít vận động",
    "lightly_active": "vận động nhẹ",
    "light": "vận động nhẹ",
    "moderately_active": "vận động vừa",
    "moderate": "vận động vừa",
    "very_active": "vận động nhiều",
    "active": "vận động nhiều",
    "extra_active": "vận động rất nhiều",
}
_GOAL_VI = {
    "lose_weight": "giảm cân",
    "gain_muscle": "tăng cơ",
    "maintain": "duy trì cân nặng",
    "gain_weight": "tăng cân",
}
_DIET_VI = {
    "keto": "keto (ít carb)",
    "vegan": "thuần chay",
    "vegetarian": "ăn chay",
    "paleo": "paleo",
    "balanced": "cân bằng",
    "high_protein": "nhiều đạm",
}
_MEAL_VI = {
    "BREAKFAST": "Bữa sáng",
    "LUNCH": "Bữa trưa",
    "DINNER": "Bữa tối",
    "SNACK": "Bữa phụ",
}
_MUSCLE_VI = {
    "CHEST": "ngực",
    "BACK": "lưng",
    "LEGS": "chân",
    "SHOULDERS": "vai",
    "ARMS": "tay",
    "CORE": "bụng",
    "CARDIO": "cardio",
    "FULL_BODY": "toàn thân",
}
_INTENSITY_VI = {"LIGHT": "nhẹ", "MODERATE": "vừa", "HEAVY": "nặng"}


def _age_from_birthdate(birth_date_str: str | None) -> str:
    if not birth_date_str:
        return "chưa rõ"
    try:
        parts = birth_date_str[:10].split("-")
        birth_year = int(parts[0])
        age = date.today().year - birth_year
        return str(age)
    except Exception:
        return "chưa rõ"


def format_user_profile_document(profile, metric) -> str:
    lines = []

    if profile:
        gender = _GENDER_VI.get(profile.gender or "", profile.gender or "chưa rõ")
        age = _age_from_birthdate(profile.birthDate)
        height = f"{profile.heightCm}cm" if profile.heightCm else "chưa rõ"
        init_weight = f"{profile.initialWeightKg}kg" if profile.initialWeightKg else "chưa rõ"
        activity = _ACTIVITY_VI.get(profile.activityLevel or "", profile.activityLevel or "chưa rõ")
        goal = _GOAL_VI.get(profile.goalType or "", profile.goalType or "chưa rõ")
        diet = _DIET_VI.get(profile.dietType or "", profile.dietType or "không hạn chế")
        target_weight = f"{profile.targetWeightKg}kg" if profile.targetWeightKg else "chưa đặt"
        deadline = profile.goalDeadline or "chưa đặt"

        lines.append(f"Người dùng: {gender}, {age} tuổi, cao {height}, cân nặng ban đầu {init_weight}.")
        lines.append(f"Mức độ hoạt động: {activity}.")
        lines.append(f"Mục tiêu: {goal}, cân nặng mục tiêu {target_weight}, deadline {deadline}.")
        lines.append(f"Chế độ ăn: {diet}.")

        if profile.foodAllergies:
            allergies = ", ".join(a for a in profile.foodAllergies if a != "NONE")
            if allergies:
                lines.append(f"Dị ứng/kiêng: {allergies}.")

        goals = []
        if profile.dailyCaloriesGoal:
            goals.append(f"calo {profile.dailyCaloriesGoal:.0f} kcal")
        if profile.proteinGoalG:
            goals.append(f"đạm {profile.proteinGoalG:.0f}g")
        if profile.fatGoalG:
            goals.append(f"béo {profile.fatGoalG:.0f}g")
        if profile.carbsGoalG:
            goals.append(f"tinh bột {profile.carbsGoalG:.0f}g")
        if profile.waterGoalMl:
            goals.append(f"nước {profile.waterGoalMl}ml")
        if goals:
            lines.append(f"Mục tiêu hàng ngày: {', '.join(goals)}.")

        if profile.weeklyRateKg:
            lines.append(f"Tốc độ thay đổi cân nặng mục tiêu: {profile.weeklyRateKg}kg/tuần.")

    if metric:
        weight = f"{metric.weightKg}kg" if metric.weightKg else "chưa đo"
        bmi = f"{metric.bmi:.1f}" if metric.bmi else "chưa tính"
        bmr = f"{metric.bmr:.0f} kcal" if metric.bmr else "chưa tính"
        tdee = f"{metric.tdee:.0f} kcal" if metric.tdee else "chưa tính"
        body_fat = f"{metric.bodyFatPct}%" if metric.bodyFatPct else "chưa đo"
        lines.append(f"Chỉ số hiện tại: cân nặng {weight}, BMI {bmi}, % mỡ {body_fat}.")
        lines.append(f"BMR {bmr}/ngày, TDEE {tdee}/ngày.")
        measurements = []
        if metric.waistCm:
            measurements.append(f"vòng eo {metric.waistCm}cm")
        if metric.hipCm:
            measurements.append(f"vòng hông {metric.hipCm}cm")
        if metric.chestCm:
            measurements.append(f"vòng ngực {metric.chestCm}cm")
        if measurements:
            lines.append(f"Số đo: {', '.join(measurements)}.")

    if not lines:
        lines.append("Chưa có thông tin hồ sơ sức khỏe.")

    return "\n".join(lines)


def format_daily_meal_summary(day_data: dict) -> str:
    date_str = day_data.get("date", "")
    total_cal = day_data.get("total_calories", 0)
    total_prot = day_data.get("total_protein", 0)
    total_carbs = day_data.get("total_carbs", 0)
    total_fat = day_data.get("total_fat", 0)
    total_fiber = day_data.get("total_fiber", 0)
    meals = day_data.get("meals", [])

    lines = [f"Ngày {date_str}: tổng {total_cal:.0f} kcal."]

    meal_parts = []
    for meal in meals:
        meal_type_vi = _MEAL_VI.get(meal.get("type", "").upper(), meal.get("type", ""))
        meal_parts.append(f"{meal_type_vi} ({meal.get('calories', 0):.0f} kcal)")
    if meal_parts:
        lines.append("Các bữa: " + ", ".join(meal_parts) + ".")

    lines.append(
        f"Macro: đạm {total_prot:.0f}g, tinh bột {total_carbs:.0f}g, "
        f"béo {total_fat:.0f}g, chất xơ {total_fiber:.0f}g."
    )
    return "\n".join(lines)


def format_workout_session(session_data: dict) -> str:
    date_str = session_data.get("date", "")
    name = session_data.get("session_name") or "Buổi tập"
    duration = session_data.get("total_duration_minutes", 0)
    calories = session_data.get("total_calories_burned", 0)
    exercises = session_data.get("exercises", [])
    muscle_groups = session_data.get("muscle_groups", [])

    muscles_vi = [_MUSCLE_VI.get(m, m) for m in muscle_groups]
    lines = [
        f"{name} ngày {date_str}: {duration} phút, đốt {calories:.0f} kcal.",
    ]
    if muscles_vi:
        lines.append(f"Nhóm cơ: {', '.join(muscles_vi)}.")

    ex_parts = []
    for ex in exercises[:8]:
        ex_name = ex.get("name", "")
        intensity_vi = _INTENSITY_VI.get(ex.get("intensity") or "", "")
        sets = ex.get("sets")
        reps = ex.get("reps_per_set")
        weight = ex.get("weight_kg")
        muscle_vi = _MUSCLE_VI.get(ex.get("muscle_group") or "", "")

        detail_parts = []
        if sets and reps:
            detail_parts.append(f"{sets} set × {reps} rep")
        if weight:
            detail_parts.append(f"{weight}kg")
        if muscle_vi:
            detail_parts.append(f"cơ {muscle_vi}")
        if intensity_vi:
            detail_parts.append(f"cường độ {intensity_vi}")

        if detail_parts:
            ex_parts.append(f"{ex_name} ({', '.join(detail_parts)})")
        else:
            ex_parts.append(ex_name)

    if ex_parts:
        lines.append("Bài tập: " + "; ".join(ex_parts) + ".")

    return "\n".join(lines)
