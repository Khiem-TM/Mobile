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

  @CreateDateColumn()
  createdAt!: Date;

  @UpdateDateColumn()
  updatedAt!: Date;

  @OneToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'userId' })
  user!: User;
}
