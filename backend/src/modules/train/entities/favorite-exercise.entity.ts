import {
  Entity,
  PrimaryColumn,
  ManyToOne,
  JoinColumn,
  CreateDateColumn,
  Column,
} from 'typeorm';
import { User } from '../../user/entities/user.entity';
import { Exercise } from './exercise.entity';

@Entity('favorite_exercises')
export class FavoriteExercise {
  @PrimaryColumn({ name: 'user_id', type: 'uuid' })
  userId!: string;

  @PrimaryColumn({ name: 'exercise_id', type: 'uuid' })
  exerciseId!: string;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user!: User;

  @ManyToOne(() => Exercise, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'exercise_id' })
  exercise!: Exercise;

  @CreateDateColumn({ name: 'created_at' })
  createdAt!: Date;
}
