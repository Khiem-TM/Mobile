import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
  ManyToOne,
  JoinColumn,
} from 'typeorm';
import { Exercise } from './exercise.entity';
import { User } from '../../users/entities/user.entity';

@Entity('workout_sessions')
export class WorkoutSession {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ name: 'user_id' })
  userId!: string;

  @Column({ name: 'exercise_id' })
  exerciseId!: string;

  @Column({ name: 'session_date', type: 'date' })
  sessionDate!: string;

  @Column({ name: 'duration_minutes', default: 0 })
  durationMinutes!: number;

  @Column({ name: 'weight_kg', type: 'decimal', precision: 6, scale: 2, nullable: true })
  weightKg!: number;

  @Column({ default: 0 })
  sets!: number;

  @Column({ name: 'reps_per_set', default: 0 })
  repsPerSet!: number;

  @Column({ name: 'calories_burned_snapshot', type: 'decimal', precision: 7, scale: 2 })
  caloriesBurnedSnapshot!: number;

  @Column({ type: 'text', nullable: true })
  notes!: string;

  @CreateDateColumn()
  createdAt!: Date;

  @UpdateDateColumn()
  updatedAt!: Date;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user!: User;

  @ManyToOne(() => Exercise, { onDelete: 'RESTRICT' })
  @JoinColumn({ name: 'exercise_id' })
  exercise!: Exercise;
}
