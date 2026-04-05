import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
} from 'typeorm';

@Entity('password_resets')
export class PasswordReset {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ name: 'user_id' })
  userId!: string;

  @Column({ unique: true })
  token!: string;

  @Column({ name: 'expires_at' })
  expiresAt!: Date;

  @Column({ name: 'used_at', nullable: true })
  usedAt!: Date;

  @CreateDateColumn({ name: 'created_at' })
  createdAt!: Date;
}
