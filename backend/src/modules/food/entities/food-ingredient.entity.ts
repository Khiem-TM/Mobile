import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  ManyToOne,
  JoinColumn,
} from 'typeorm';
import { Food } from './food.entity';

@Entity('food_ingredients')
export class FoodIngredient {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ type: 'uuid', name: 'custom_food_id' })
  custom_food_id!: string;

  @ManyToOne(() => Food, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'custom_food_id' })
  customFood!: Food;

  @Column({ type: 'uuid', nullable: true, name: 'ingredient_food_id' })
  ingredient_food_id!: string | null;

  @ManyToOne(() => Food, { nullable: true, onDelete: 'SET NULL' })
  @JoinColumn({ name: 'ingredient_food_id' })
  ingredientFood!: Food | null;

  // Manual name when ingredient_food_id is null
  @Column({ type: 'varchar', length: 255, nullable: true, name: 'ingredient_name' })
  ingredient_name!: string | null;

  @Column({ type: 'decimal', precision: 8, scale: 2, name: 'quantity_g' })
  quantity_g!: number;

  // Flat overrides used when ingredient_food_id is null (total for quantity_g, not per-100g)
  @Column({ type: 'decimal', precision: 8, scale: 2, nullable: true, name: 'calories_override' })
  calories_override!: number | null;

  @Column({ type: 'decimal', precision: 7, scale: 2, nullable: true, name: 'protein_override' })
  protein_override!: number | null;

  @Column({ type: 'decimal', precision: 7, scale: 2, nullable: true, name: 'fat_override' })
  fat_override!: number | null;

  @Column({ type: 'decimal', precision: 7, scale: 2, nullable: true, name: 'carbs_override' })
  carbs_override!: number | null;

  @CreateDateColumn({ name: 'created_at' })
  created_at!: Date;
}
