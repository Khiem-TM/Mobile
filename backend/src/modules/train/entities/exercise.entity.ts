import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
} from 'typeorm';
import { ExerciseType } from '../enums/exercise-type.enum';
import { DifficultyLevel } from '../enums/difficulty-level.enum';
import { IntensityLevel } from '../enums/intensity-level.enum';

@Entity('exercises')
export class Exercise {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ type: 'varchar', length: 100 })
  name!: string;

  @Column({ type: 'text', nullable: true })
  description!: string;

  @Column({ type: 'text', nullable: true })
  instructions!: string;

  @Column({
    name: 'exercise_type',
    type: 'varchar',
    length: 10,
    default: ExerciseType.SPORT,
  })
  exerciseType!: ExerciseType;

  @Column({ type: 'varchar', length: 50, nullable: true })
  category!: string | null;

  @Column({
    name: 'muscle_group',
    type: 'varchar',
    length: 50,
    nullable: true,
  })
  muscleGroup!: string | null;

  @Column({
    name: 'difficulty_level',
    type: 'varchar',
    length: 20,
    default: DifficultyLevel.BEGINNER,
  })
  difficultyLevel!: DifficultyLevel;

  @Column({ name: 'met_value', type: 'decimal', precision: 5, scale: 2, default: 0 })
  metValue!: number;

  @Column({ name: 'video_url', type: 'text', nullable: true })
  videoUrl!: string | null;

  @Column({ name: 'image_avt_url', type: 'text', nullable: true, default: null })
  imageAvtUrl!: string | null;

  @Column({ name: 'image_avt_public_id', type: 'varchar', length: 255, nullable: true, default: null })
  imageAvtPublicId!: string | null;

  @Column({ name: 'image_url', type: 'text', array: true, nullable: true, default: null })
  imageUrl!: string[] | null;

  @Column({ name: 'image_public_ids', type: 'text', array: true, nullable: true, default: null })
  imagePublicIds!: string[] | null;

  @Column({ name: 'favorites_count', type: 'int', default: 0 })
  favoritesCount!: number;

  @Column({ name: 'secondary_muscle_groups', type: 'simple-json', nullable: true, default: null })
  secondaryMuscleGroups!: string[] | null;

  @Column({ type: 'varchar', length: 30, nullable: true, default: null })
  equipment!: string | null;

  @Column({ name: 'form_tips', type: 'text', nullable: true })
  formTips!: string | null;

  @Column({ name: 'is_active', type: 'boolean', default: true })
  isActive!: boolean;

  // ─── GYM-specific fields ─────────────────────────────────────────────────
  @Column({ name: 'default_sets', type: 'int', nullable: true })
  defaultSets!: number | null;

  @Column({ name: 'default_reps', type: 'int', nullable: true })
  defaultReps!: number | null;

  @Column({ name: 'default_weight_kg', type: 'decimal', precision: 6, scale: 2, nullable: true })
  defaultWeightKg!: number | null;

  @Column({ name: 'target_muscle_group', type: 'varchar', length: 50, nullable: true })
  targetMuscleGroup!: string | null;

  @Column({ name: 'rest_time_seconds', type: 'int', nullable: true })
  restTimeSeconds!: number | null;

  // ─── SPORT-specific fields ────────────────────────────────────────────────
  @Column({ name: 'default_duration_minutes', type: 'int', nullable: true })
  defaultDurationMinutes!: number | null;

  @Column({
    name: 'default_intensity_level',
    type: 'varchar',
    length: 10,
    nullable: true,
  })
  defaultIntensityLevel!: IntensityLevel | null;

  @Column({ name: 'movement_type', type: 'varchar', length: 50, nullable: true })
  movementType!: string | null;

  @Column({ name: 'estimated_calories_per_minute', type: 'decimal', precision: 5, scale: 2, nullable: true })
  estimatedCaloriesPerMinute!: number | null;

  @CreateDateColumn()
  createdAt!: Date;

  @UpdateDateColumn()
  updatedAt!: Date;
}
