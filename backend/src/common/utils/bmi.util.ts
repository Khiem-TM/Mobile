/**
 * Calculate BMI: weight (kg) / (height (m) ^ 2)
 * - BMI < 18.5: Underweight
 * - 18.5 <= BMI < 25: Normal weight
 * - 25 <= BMI < 30: Overweight
 * - BMI >= 30: Obesity
 */
export class BMIUtil {
  static calculate(weightKg: number, heightCm: number): number {
    if (!heightCm || heightCm <= 0) return 0;
    const heightM = heightCm / 100;
    return Number((weightKg / (heightM * heightM)).toFixed(2));
  }
}
