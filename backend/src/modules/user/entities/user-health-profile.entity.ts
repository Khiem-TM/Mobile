import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
  OneToOne,
  JoinColumn,
} from 'typeorm';
import { User } from './user.entity';
import { FoodAllergyType } from '../../../common/enums/food-allergy.enum';

@Entity('user_health_profiles')
export class UserHealthProfile {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column()
  userId!: string;

  @Column({
    name: 'food_allergies',
    type: 'enum',
    enum: FoodAllergyType,
    array: true,
    default: [],
  })
  foodAllergies!: FoodAllergyType[];

  @Column({ name: 'birth_date', type: 'date' })
  birthDate!: string;

  @Column({ type: 'varchar', length: 10 })
  gender!: string;

  @Column({ name: 'height_cm', type: 'decimal', precision: 5, scale: 1 })
  heightCm!: number;

  @Column({
    name: 'initial_weight_kg',
    type: 'decimal',
    precision: 5,
    scale: 1,
  })
  initialWeightKg!: number;

  @Column({ name: 'activity_level', type: 'varchar', length: 20 })
  activityLevel!: string;

  @Column({ name: 'diet_type', type: 'varchar', length: 30, nullable: true })
  dietType!: string;

  @Column({
    name: 'weight_goal_kg',
    type: 'decimal',
    precision: 5,
    scale: 1,
    nullable: true,
  })
  weightGoalKg!: number;

  @Column({ name: 'water_goal_ml', type: 'int', nullable: true, default: 2000 })
  waterGoalMl!: number;

  @Column({
    name: 'calories_goal',
    type: 'decimal',
    precision: 7,
    scale: 2,
    nullable: true,
  })
  caloriesGoal!: number;

  @Column({ name: 'goal_type', type: 'varchar', length: 30, nullable: true })
  goalType!: string | null;

  @Column({ name: 'target_weight_kg', type: 'decimal', precision: 5, scale: 1, nullable: true })
  targetWeightKg!: number | null;

  @Column({ name: 'daily_calories_goal', type: 'decimal', precision: 7, scale: 2, nullable: true })
  dailyCaloriesGoal!: number | null;

  @Column({ name: 'protein_goal_g', type: 'decimal', precision: 6, scale: 2, nullable: true })
  proteinGoalG!: number | null;

  @Column({ name: 'fat_goal_g', type: 'decimal', precision: 6, scale: 2, nullable: true })
  fatGoalG!: number | null;

  @Column({ name: 'carbs_goal_g', type: 'decimal', precision: 6, scale: 2, nullable: true })
  carbsGoalG!: number | null;

  @Column({ name: 'weekly_rate_kg', type: 'decimal', precision: 4, scale: 2, nullable: true })
  weeklyRateKg!: number | null;

  @Column({ name: 'goal_start_date', type: 'date', nullable: true })
  goalStartDate!: string | null;

  @Column({ name: 'goal_deadline', type: 'date', nullable: true })
  goalDeadline!: string | null;

  @Column({ name: 'goal_status', type: 'varchar', length: 20, nullable: true })
  goalStatus!: string | null;

  @CreateDateColumn()
  createdAt!: Date;

  @UpdateDateColumn()
  updatedAt!: Date;

  @OneToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'userId' })
  user!: User;
}
