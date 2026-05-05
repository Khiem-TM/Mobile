import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  ManyToOne,
  JoinColumn,
} from 'typeorm';
import { FoodRecipe } from './food-recipe.entity';

@Entity('food_recipe_steps')
export class FoodRecipeStep {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid' })
  recipe_id: string;

  @ManyToOne(() => FoodRecipe, (recipe) => recipe.steps, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'recipe_id' })
  recipe: FoodRecipe;

  @Column({ type: 'int' })
  step_number: number;

  @Column({ type: 'text' })
  instruction: string;

  @Column({ type: 'text', nullable: true })
  image_url: string | null;

  @Column({ type: 'varchar', length: 255, nullable: true })
  image_public_id: string | null;

  @CreateDateColumn()
  created_at: Date;
}
