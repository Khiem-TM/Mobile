import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
  ManyToOne,
  OneToMany,
  JoinColumn,
  Unique,
} from 'typeorm';
import { User } from '../../user/entities/user.entity';
import { TrainingSessionItem } from './training-session-item.entity';

@Entity('training_sessions')
@Unique(['userId', 'sessionDate'])
export class TrainingSession {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ name: 'user_id' })
  userId!: string;

  @Column({ name: 'session_date', type: 'date' })
  sessionDate!: string;

  @Column({ type: 'varchar', length: 100, nullable: true })
  title!: string | null;

  @Column({ type: 'text', nullable: true })
  note!: string | null;

  @Column({ name: 'total_duration_minutes', type: 'int', default: 0 })
  totalDurationMinutes!: number;

  @Column({ name: 'total_calories_burned', type: 'decimal', precision: 7, scale: 2, default: 0 })
  totalCaloriesBurned!: number;

  @CreateDateColumn()
  createdAt!: Date;

  @UpdateDateColumn()
  updatedAt!: Date;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user!: User;

  @OneToMany(() => TrainingSessionItem, (item) => item.session, { cascade: true })
  items!: TrainingSessionItem[];
}
