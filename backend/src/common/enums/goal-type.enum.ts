export enum GoalType {
  LOSE_WEIGHT = 'lose_weight',
  GAIN_WEIGHT = 'gain_weight',
  MAINTAIN = 'maintain',
  BULKING = 'bulking', // tăng cơ
  CUTTING = 'cutting', // siết cơ
}

// idea:
/*
- LOSE_WEIGHT: Mục tiêu giảm cân --> Calo < TDEE
- GAIN_WEIGHT: Mục tiêu tăng cân --> Calo > TDEE
- MAINTAIN: Mục tiêu duy trì cân nặng --> Calo = TDEE
- BULKING: Mục tiêu tăng cơ --> Calo > TDEE + protein cao
- CUTTING: Mục tiêu siết cơ --> Calo < TDEE - protein cao --> Giảm mỡ nhưng vẫn duy trì cơ bắp
*/
