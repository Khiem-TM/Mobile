import {
  Entity,
  CreateDateColumn,
  ManyToOne,
  JoinColumn,
  PrimaryColumn,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';
import { Food } from './food.entity';

@Entity('food_user_favorites')
export class FoodUserFavorite {
  @PrimaryColumn('uuid')
  user_id: string;

  @PrimaryColumn('uuid')
  food_id: string;

  @ManyToOne(() => User)
  @JoinColumn({ name: 'user_id' })
  user: User;

  @ManyToOne(() => Food)
  @JoinColumn({ name: 'food_id' })
  food: Food;

  @CreateDateColumn()
  created_at: Date;
}
