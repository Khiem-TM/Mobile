import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  ManyToOne,
  JoinColumn,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';
import { BodyMetric } from './body-metric.entity';

@Entity('body_progress_photos')
export class BodyProgressPhoto {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ name: 'user_id' })
  userId!: string;

  @Column({ name: 'body_metric_id', nullable: true })
  bodyMetricId!: string;

  @Column({ type: 'text' })
  photoUrl!: string;

  @Column({ type: 'varchar', length: 255, nullable: true, default: null })
  photoPublicId!: string | null;

  @Column({ default: 'front' })
  photoType!: string;

  @CreateDateColumn()
  takenAt!: Date;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user!: User;

  @ManyToOne(() => BodyMetric, { nullable: true, onDelete: 'SET NULL' })
  @JoinColumn({ name: 'body_metric_id' })
  bodyMetric!: BodyMetric;
}
