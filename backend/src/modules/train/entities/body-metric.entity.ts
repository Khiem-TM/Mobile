import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  ManyToOne,
  OneToMany,
  JoinColumn,
} from 'typeorm';
import { User } from '../../user/entities/user.entity';
import { BodyProgressPhoto } from './body-progress-photo.entity';

@Entity('body_metrics')
export class BodyMetric {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ name: 'user_id' })
  userId!: string;

  @Column({
    name: 'measured_at',
    type: 'timestamptz',
    default: () => 'CURRENT_TIMESTAMP',
  })
  measuredAt!: Date;

  @Column({
    name: 'measured_date',
    type: 'date',
    nullable: true,
  })
  measuredDate!: string | null;

  @Column({
    name: 'weight_kg',
    type: 'decimal',
    precision: 5,
    scale: 1,
    nullable: true,
  })
  weightKg!: number | null;

  @Column({
    name: 'body_fat_percent',
    type: 'decimal',
    precision: 4,
    scale: 1,
    nullable: true,
  })
  bodyFatPercent!: number | null;

  // auto compute BMI, BMR, TDEE based on weight
  @Column({
    name: 'bmi',
    type: 'decimal',
    precision: 5,
    scale: 2,
    nullable: true,
  })
  bmi!: number | null;

  @Column({
    name: 'bmr',
    type: 'decimal',
    precision: 7,
    scale: 2,
    nullable: true,
  })
  bmr!: number | null;

  @Column({
    name: 'tdee',
    type: 'decimal',
    precision: 7,
    scale: 2,
    nullable: true,
  })
  tdee!: number | null;

  // Các chỉ số này đều là Optional --> Chỉ tập trung vào cân nặng và tỉ lệ mỡ, các chỉ số tự động tính
  @Column({
    name: 'waist_cm',
    type: 'decimal',
    precision: 5,
    scale: 2,
    nullable: true,
  })
  waistCm!: number | null;

  @Column({
    name: 'hip_cm',
    type: 'decimal',
    precision: 5,
    scale: 1,
    nullable: true,
  })
  hipCm!: number | null;

  @Column({
    name: 'chest_cm',
    type: 'decimal',
    precision: 5,
    scale: 1,
    nullable: true,
  })
  chestCm!: number | null;

  @Column({
    name: 'neck_cm',
    type: 'decimal',
    precision: 5,
    scale: 1,
    nullable: true,
  })
  neckCm!: number | null;

  @Column({
    name: 'height_cm',
    type: 'decimal',
    precision: 5,
    scale: 1,
    nullable: true,
  })
  heightCm!: number | null;

  @Column({
    name: 'arm_cm',
    type: 'decimal',
    precision: 5,
    scale: 1,
    nullable: true,
  })
  armCm!: number | null;

  @Column({ type: 'text', nullable: true })
  notes!: string | null;

  @CreateDateColumn({ name: 'created_at' })
  createdAt!: Date;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user!: User;

  @OneToMany(() => BodyProgressPhoto, (photo) => photo.bodyMetric)
  photos!: BodyProgressPhoto[];
}
