/**
 * Calculate BMI: weight (kg) / (height (m) ^ 2)
 */
export class BMIUtil {
  static calculate(weightKg: number, heightCm: number): number {
    if (!heightCm || heightCm <= 0) return 0;
    const heightM = heightCm / 100;
    return Number((weightKg / (heightM * heightM)).toFixed(2));
  }
}
