import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
} from 'typeorm';

@Entity('email_verifications')
export class EmailVerification {
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
