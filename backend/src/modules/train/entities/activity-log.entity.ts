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

  @Column({ name: 'water_ml', default: 0 })
  waterMl!: number;

  @Column({ type: 'text', nullable: true })
  note!: string | null;

  @Column({ name: 'sleep_hours', type: 'decimal', precision: 4, scale: 2, nullable: true })
  sleepHours!: number | null;

  @Column({ type: 'varchar', length: 20, nullable: true })
  mood!: string | null;

  @CreateDateColumn({ name: 'created_at' })
  createdAt!: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt!: Date;
}
