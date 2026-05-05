import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
  ManyToOne,
  OneToMany,
  JoinColumn,
} from 'typeorm';
import { User } from '../../user/entities/user.entity';
import { WorkoutSessionDetail } from './workout-session-detail.entity';

@Entity('workout_sessions')
export class WorkoutSession {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ name: 'user_id' })
  userId!: string;

  @Column({ name: 'session_date', type: 'date' })
  sessionDate!: string;

  @Column({ name: 'session_name', type: 'varchar', length: 100, nullable: true })
  sessionName!: string | null;

  @Column({ name: 'total_duration_minutes', type: 'int', default: 0 })
  totalDurationMinutes!: number;

  @Column({ name: 'total_calories_burned', type: 'decimal', precision: 7, scale: 2, default: 0 })
  totalCaloriesBurned!: number;

  @Column({ type: 'text', nullable: true })
  notes!: string | null;

  @CreateDateColumn()
  createdAt!: Date;

  @UpdateDateColumn()
  updatedAt!: Date;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user!: User;

  @OneToMany(() => WorkoutSessionDetail, (d) => d.session, { cascade: true })
  details!: WorkoutSessionDetail[];
}
