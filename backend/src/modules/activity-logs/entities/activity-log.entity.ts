import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
  Unique,
} from 'typeorm';

@Entity('activity_logs')
@Unique(['userId', 'logDate'])
export class ActivityLog {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ name: 'user_id' })
  userId!: string;

  @Column({ name: 'log_date', type: 'date' })
  logDate!: string;

  @Column({ default: 0 })
  steps!: number;

  @Column({
    name: 'calories_burned',
    type: 'decimal',
    precision: 7,
    scale: 2,
    default: 0,
  })
  caloriesBurned!: number;

  @Column({ name: 'active_minutes', default: 0 })
  activeMinutes!: number;

  @Column({ name: 'water_ml', default: 0 })
  waterMl!: number;

  @Column({ name: 'exercise_notes', nullable: true })
  exerciseNotes!: string;

  @CreateDateColumn({ name: 'created_at' })
  createdAt!: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt!: Date;
}
