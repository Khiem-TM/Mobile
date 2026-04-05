export class TDEEUtil {
  // eslint-disable-next-line prettier/prettier
  static calculateBMR(
    weightKg: number,
    heightCm: number,
    age: number,
    gender: string,
  ): number {
    if (gender.toLowerCase() === 'male') {
      return 10 * weightKg + 6.25 * heightCm - 5 * age + 5;
    } else {
      return 10 * weightKg + 6.25 * heightCm - 5 * age - 161;
    }
  }

  static getActivityMultiplier(level: string): number {
    const levels: Record<string, number> = {
      sedentary: 1.2,
      lightly_active: 1.375,
      moderately_active: 1.55,
      very_active: 1.725,
      extra_active: 1.9,
    };
    return levels[level.toLowerCase()] || 1.2;
  }

  static calculateTDEE(bmr: number, activityLevel: string): number {
    return bmr * this.getActivityMultiplier(activityLevel);
  }
}
