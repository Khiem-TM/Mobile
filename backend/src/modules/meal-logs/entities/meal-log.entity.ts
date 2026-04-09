import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
  ManyToOne,
  JoinColumn,
  OneToMany,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';
import { MealLogItem } from './meal-log-item.entity';
import { MealType } from '../../../common/enums/meal-type.enum';

@Entity('meal_logs')
export class MealLog {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid' })
  user_id: string;

  @ManyToOne(() => User)
  @JoinColumn({ name: 'user_id' })
  user: User;

  @Column({ type: 'timestamptz', default: () => 'CURRENT_TIMESTAMP' })
  log_date: Date;

  @Column({ type: 'enum', enum: MealType })
  meal_type: MealType;

  @Column({ type: 'text', nullable: true })
  notes: string;

  @Column({ type: 'text', nullable: true, default: null })
  image_url: string | null = null;

  @Column({ type: 'varchar', length: 255, nullable: true, default: null })
  image_public_id: string | null = null;

  @OneToMany(() => MealLogItem, (item: MealLogItem) => item.meal_log, { cascade: true })
  items: MealLogItem[];

  @CreateDateColumn()
  created_at: Date;

  @UpdateDateColumn()
  updated_at: Date;
}
