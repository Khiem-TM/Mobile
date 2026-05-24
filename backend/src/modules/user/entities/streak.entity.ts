import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
  Unique,
  ManyToOne,
  JoinColumn,
} from 'typeorm';
import { StreakType } from '../../../common/enums/streak-type.enum';
import { User } from './user.entity';

@Entity('streaks')
@Unique(['user_id', 'streak_type'])
export class Streak {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ nullable: true })
  user_id!: string | null;

  @Column({
    type: 'enum',
    enum: StreakType,
    default: StreakType.LOGIN,
  })
  streak_type!: StreakType;

  @Column({ default: 0 })
  current_streak!: number;

  @Column({ default: 0 })
  longest_streak!: number;

  @Column({ type: 'date', nullable: true })
  last_activity_date!: string;

  @CreateDateColumn()
  created_at!: Date;

  @UpdateDateColumn()
  updated_at!: Date;

  @ManyToOne(() => User, { onDelete: 'CASCADE', nullable: true })
  @JoinColumn({ name: 'user_id' })
  user!: User | null;
}
