import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
  OneToOne,
} from 'typeorm';
import { Exclude } from 'class-transformer';

@Entity('users')
export class User {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ type: 'varchar', length: 255, unique: true })
  email!: string;

  @Exclude()
  @Column({ type: 'varchar', length: 255, nullable: true })
  password_hash!: string;

  @Column({ type: 'varchar', length: 100 })
  display_name!: string;

  @Column({ type: 'text', nullable: true })
  avatar_url!: string;

  @Column({ type: 'varchar', length: 20, nullable: true })
  oauth_provider!: 'google' | 'facebook' | null;

  @Column({ type: 'varchar', length: 255, nullable: true })
  oauth_id!: string;

  @Column({ type: 'varchar', length: 20, default: 'user' })
  role!: 'user' | 'admin';

  @Column({ type: 'boolean', default: false })
  is_verified!: boolean;

  @Column({ type: 'boolean', default: true })
  is_active!: boolean;

  @CreateDateColumn()
  created_at!: Date;

  @UpdateDateColumn()
  updated_at!: Date;

  @OneToOne('UserHealthProfile', 'user')
  healthProfile!: any;
}
