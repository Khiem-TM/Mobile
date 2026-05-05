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
import { Food } from './food.entity';
import { FoodRecipeStep } from './food-recipe-step.entity';

@Entity('food_recipes')
export class FoodRecipe {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid' })
  food_id: string;

  @ManyToOne(() => Food, (food) => food.recipes, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'food_id' })
  food: Food;

  @Column({ type: 'int', nullable: true })
  prep_time_min: number | null;

  @Column({ type: 'int', nullable: true })
  cook_time_min: number | null;

  @Column({ type: 'int', nullable: true })
  servings: number | null;

  @CreateDateColumn()
  created_at: Date;

  @UpdateDateColumn()
  updated_at: Date;

  @OneToMany(() => FoodRecipeStep, (step) => step.recipe, { cascade: true })
  steps: FoodRecipeStep[];
}
