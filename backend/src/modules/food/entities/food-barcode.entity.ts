import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  ManyToOne,
  JoinColumn,
} from 'typeorm';
import { Food } from './food.entity';

@Entity('food_barcodes')
export class FoodBarcode {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid' })
  food_id: string;

  @ManyToOne(() => Food)
  @JoinColumn({ name: 'food_id' })
  food: Food;

  @Column({ type: 'varchar', length: 50, unique: true })
  barcode: string;

  @Column({ type: 'varchar', length: 20, default: 'EAN13' })
  barcode_type: string;

  @CreateDateColumn()
  created_at: Date;
}
