import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  ManyToOne,
  OneToMany,
  JoinColumn,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';

@Entity('body_metrics')
export class BodyMetric {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ name: 'user_id' })
  userId!: string;

  @Column({ name: 'recorded_at', type: 'timestamptz', default: () => 'CURRENT_TIMESTAMP' })
  recordedAt!: Date;

  @Column({
    name: 'weight_kg',
    type: 'decimal',
    precision: 5,
    scale: 1,
    nullable: true,
  })
  weightKg!: number;

  @Column({
    name: 'body_fat_pct',
    type: 'decimal',
    precision: 4,
    scale: 1,
    nullable: true,
  })
  bodyFatPct!: number;

  @Column({
    name: 'bmi',
    type: 'decimal',
    precision: 5,
    scale: 2,
    nullable: true,
  })
  bmi!: number;

  @Column({
    name: 'bmr',
    type: 'decimal',
    precision: 7,
    scale: 2,
    nullable: true,
  })
  bmr!: number;

  @Column({
    name: 'tdee',
    type: 'decimal',
    precision: 7,
    scale: 2,
    nullable: true,
  })
  tdee!: number;

  @Column({
    name: 'waist_cm',
    type: 'decimal',
    precision: 5,
    scale: 2,
    nullable: true,
  })
  waistCm!: number;

  @Column({
    name: 'hip_cm',
    type: 'decimal',
    precision: 5,
    scale: 1,
    nullable: true,
  })
  hipCm!: number;

  @Column({
    name: 'chest_cm',
    type: 'decimal',
    precision: 5,
    scale: 1,
    nullable: true,
  })
  chestCm!: number;

  @Column({
    name: 'neck_cm',
    type: 'decimal',
    precision: 5,
    scale: 1,
    nullable: true,
  })
  neckCm!: number;

  @Column({ nullable: true })
  notes!: string;

  @CreateDateColumn({ name: 'created_at' })
  createdAt!: Date;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user!: User;

  @OneToMany('BodyProgressPhoto', 'bodyMetric')
  photos!: any[];
}
