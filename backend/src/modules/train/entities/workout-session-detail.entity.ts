import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  ManyToOne,
  JoinColumn,
} from 'typeorm';
import { WorkoutSession } from './workout-session.entity';
import { Exercise } from './exercise.entity';

@Entity('workout_session_details')
export class WorkoutSessionDetail {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ name: 'workout_session_id' })
  workoutSessionId!: string;

  @Column({ name: 'exercise_id' })
  exerciseId!: string;

  @Column({ name: 'order_index', type: 'int', default: 0 })
  orderIndex!: number;

  @Column({ name: 'duration_minutes', type: 'int', default: 0 })
  durationMinutes!: number;

  @Column({ name: 'weight_kg', type: 'decimal', precision: 6, scale: 2, nullable: true })
  weightKg!: number | null;

  @Column({ type: 'int', nullable: true })
  sets!: number | null;

  @Column({ name: 'reps_per_set', type: 'int', nullable: true })
  repsPerSet!: number | null;

  @Column({ name: 'calories_burned', type: 'decimal', precision: 7, scale: 2, default: 0 })
  caloriesBurned!: number;

  @Column({ type: 'text', nullable: true })
  notes!: string | null;

  @CreateDateColumn()
  createdAt!: Date;

  @ManyToOne(() => WorkoutSession, (s) => s.details, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'workout_session_id' })
  session!: WorkoutSession;

  @ManyToOne(() => Exercise, { onDelete: 'RESTRICT', eager: false })
  @JoinColumn({ name: 'exercise_id' })
  exercise!: Exercise;
}
