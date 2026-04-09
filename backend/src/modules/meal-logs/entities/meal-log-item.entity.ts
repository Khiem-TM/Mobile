import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  ManyToOne,
  JoinColumn,
} from 'typeorm';
import { MealLog } from './meal-log.entity';
import { Food } from '../../foods/entities/food.entity';

@Entity('meal_log_items')
export class MealLogItem {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ type: 'uuid' })
  meal_log_id!: string;

  @ManyToOne(() => MealLog, (mealLog: MealLog) => mealLog.items, {
    onDelete: 'CASCADE',
  })
  @JoinColumn({ name: 'meal_log_id' })
  meal_log!: MealLog;

  @Column({ type: 'uuid' })
  food_id!: string;

  @ManyToOne(() => Food)
  @JoinColumn({ name: 'food_id' })
  food: Food;

  @Column({ type: 'decimal', precision: 7, scale: 2 })
  quantity: number;

  @Column({ type: 'varchar', length: 50 })
  serving_unit: string;

  @Column({ type: 'decimal', precision: 8, scale: 2 })
  quantity_in_grams: number;

  @Column({ type: 'decimal', precision: 8, scale: 2 })
  calories_snapshot: number;

  @Column({ type: 'decimal', precision: 7, scale: 2 })
  protein_snapshot: number;

  @Column({ type: 'decimal', precision: 7, scale: 2 })
  fat_snapshot: number;

  @Column({ type: 'decimal', precision: 7, scale: 2 })
  carbs_snapshot: number;

  @Column({ type: 'decimal', precision: 7, scale: 2, nullable: true })
  fiber_snapshot: number;

  @Column({ type: 'decimal', precision: 7, scale: 2, nullable: true })
  sugar_snapshot: number;

  @Column({ type: 'decimal', precision: 7, scale: 2, nullable: true })
  sodium_snapshot: number;

  @Column({ type: 'varchar', length: 20, default: 'manual' })
  source: 'manual' | 'ai_scan' | 'barcode' | 'history' | 'favorite';

  @CreateDateColumn()
  created_at: Date;
}
