import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  ManyToOne,
  JoinColumn,
} from 'typeorm';
import { TrainingSession } from './training-session.entity';
import { Exercise } from './exercise.entity';
import { ExerciseType } from '../enums/exercise-type.enum';
import { IntensityLevel } from '../enums/intensity-level.enum';

@Entity('training_session_items')
export class TrainingSessionItem {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ name: 'session_id' })
  sessionId!: string;

  @Column({ name: 'exercise_id' })
  exerciseId!: string;

  @Column({
    name: 'exercise_type',
    type: 'varchar',
    length: 10,
    default: ExerciseType.SPORT,
  })
  exerciseType!: ExerciseType;

  @Column({ name: 'order_index', type: 'int', default: 0 })
  orderIndex!: number;

  @Column({ name: 'duration_minutes', type: 'int', default: 0 })
  durationMinutes!: number;

  @Column({ name: 'calories_burned', type: 'decimal', precision: 7, scale: 2, default: 0 })
  caloriesBurned!: number;

  @Column({ type: 'text', nullable: true })
  note!: string | null;

  // ─── GYM-specific ─────────────────────────────────────────────────────────
  @Column({ type: 'int', nullable: true })
  sets!: number | null;

  @Column({ type: 'int', nullable: true })
  reps!: number | null;

  @Column({ name: 'weight_kg', type: 'decimal', precision: 6, scale: 2, nullable: true })
  weightKg!: number | null;

  @Column({ name: 'rest_time_seconds', type: 'int', nullable: true })
  restTimeSeconds!: number | null;

  // ─── SPORT-specific ───────────────────────────────────────────────────────
  @Column({
    name: 'intensity_level',
    type: 'varchar',
    length: 10,
    nullable: true,
  })
  intensityLevel!: IntensityLevel | null;

  @Column({ name: 'distance_km', type: 'decimal', precision: 7, scale: 3, nullable: true })
  distanceKm!: number | null;

  // CARDIO: avg speed used to auto-compute durationMinutes and calories
  @Column({ name: 'avg_speed_kmh', type: 'decimal', precision: 5, scale: 2, nullable: true })
  avgSpeedKmh!: number | null;

  @Column({ type: 'varchar', length: 20, nullable: true })
  pace!: string | null;

  @CreateDateColumn()
  createdAt!: Date;

  @ManyToOne(() => TrainingSession, (s) => s.items, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'session_id' })
  session!: TrainingSession;

  @ManyToOne(() => Exercise, { onDelete: 'RESTRICT', eager: false })
  @JoinColumn({ name: 'exercise_id' })
  exercise!: Exercise;
}
