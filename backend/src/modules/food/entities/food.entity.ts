import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
  ManyToOne,
  JoinColumn,
} from 'typeorm';
import { User } from '../../user/entities/user.entity';

@Entity('foods')
export class Food {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'varchar', length: 255 })
  name: string;

  @Column({ type: 'varchar', length: 255, nullable: true })
  name_en: string;

  @Column({ type: 'varchar', length: 100, nullable: true })
  brand: string;

  @Column({ type: 'varchar', length: 50, nullable: true })
  category: string;

  @Column({ type: 'text', nullable: true })
  description: string | null;

  @Column({ type: 'varchar', length: 20, default: 'ingredient' })
  food_type: 'ingredient' | 'dish' | 'product';

  @Column({ type: 'int', default: 0 })
  favorites_count: number;

  @Column({ type: 'decimal', precision: 7, scale: 2 })
  serving_size_g: number;

  @Column({ type: 'varchar', length: 50, default: 'g' })
  serving_unit: string;

  // ─── Core nutrition (per 100g) ──────────────────────────────────────────────
  @Column({ type: 'decimal', precision: 7, scale: 2 })
  calories_per_100g: number;

  @Column({ type: 'decimal', precision: 7, scale: 2, default: 0 })
  protein_per_100g: number;

  @Column({ type: 'decimal', precision: 7, scale: 2, default: 0 })
  fat_per_100g: number;

  @Column({ type: 'decimal', precision: 7, scale: 2, default: 0 })
  carbs_per_100g: number;

  @Column({ type: 'decimal', precision: 7, scale: 2, nullable: true })
  fiber_per_100g: number | null;

  // ─── Media ──────────────────────────────────────────────────────────────────
  @Column({ type: 'text', array: true, nullable: true, default: null })
  image_urls: string[] | null = null;

  @Column({ type: 'text', array: true, nullable: true, default: null })
  image_public_ids: string[] | null = null;

  // ─── Ownership ──────────────────────────────────────────────────────────────
  // is_custom=false → system food visible to all users
  // is_custom=true  → personal food visible only to created_by_user_id
  @Column({ type: 'boolean', default: false })
  is_custom: boolean;

  @Column({ type: 'uuid', nullable: true })
  created_by_user_id: string | null;

  @ManyToOne(() => User)
  @JoinColumn({ name: 'created_by_user_id' })
  creator: User;

  @Column({ type: 'boolean', default: false })
  is_verified: boolean;

  @Column({ type: 'boolean', default: true })
  is_active: boolean;

  @CreateDateColumn()
  created_at: Date;

  @UpdateDateColumn()
  updated_at: Date;
}
