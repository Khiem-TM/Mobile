import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
  ManyToOne,
  JoinColumn,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';
import { TrainingGoalType } from '../../../common/enums/training-goal-type.enum';
import { TrainingGoalStatus } from '../../../common/enums/training-goal-status.enum';

@Entity('training_goals')
export class TrainingGoal {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ name: 'user_id' })
  userId!: string;

  @Column({
    name: 'goal_type',
    type: 'enum',
    enum: TrainingGoalType,
  })
  goalType!: TrainingGoalType;

  @Column({ name: 'target_value', type: 'decimal', precision: 10, scale: 2 })
  targetValue!: number;

  @Column({ name: 'current_value', type: 'decimal', precision: 10, scale: 2, default: 0 })
  currentValue!: number;

  @Column({ name: 'start_date', type: 'date' })
  startDate!: string;

  @Column({ name: 'daily_calories_goal', type: 'decimal', precision: 7, scale: 2, nullable: true })
  dailyCaloriesGoal!: number;

  @Column({ name: 'protein_g', type: 'decimal', precision: 6, scale: 2, nullable: true })
  proteinG!: number;

  @Column({ name: 'fat_g', type: 'decimal', precision: 6, scale: 2, nullable: true })
  fatG!: number;

  @Column({ name: 'carbs_g', type: 'decimal', precision: 6, scale: 2, nullable: true })
  carbsG!: number;

  @Column({ name: 'weekly_rate_kg', type: 'decimal', precision: 4, scale: 2, nullable: true })
  weeklyRateKg!: number;

  @Column({ type: 'date' })
  deadline!: string;

  @Column({
    type: 'enum',
    enum: TrainingGoalStatus,
    default: TrainingGoalStatus.ACTIVE,
  })
  status!: TrainingGoalStatus;

  @CreateDateColumn()
  createdAt!: Date;

  @UpdateDateColumn()
  updatedAt!: Date;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user!: User;
}
