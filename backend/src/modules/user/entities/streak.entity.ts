import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
  Unique,
} from 'typeorm';
import { StreakType } from '../../../common/enums/streak-type.enum';

@Entity('streaks')
@Unique(['user_id', 'streak_type'])
export class Streak {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column()
  user_id!: string;

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
}
