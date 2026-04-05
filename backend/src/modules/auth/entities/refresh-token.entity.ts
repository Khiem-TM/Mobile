import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
} from 'typeorm';

@Entity('refresh_tokens')
export class RefreshToken {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ type: 'uuid' })
  user_id!: string;

  @Column({ type: 'varchar', length: 255, unique: true })
  token_hash!: string;

  @Column({ type: 'text', nullable: true })
  device_info!: string;

  @Column({ type: 'timestamp' })
  expires_at!: Date;

  @Column({ type: 'timestamp', nullable: true })
  revoked_at!: Date;

  @CreateDateColumn()
  created_at!: Date;
}
