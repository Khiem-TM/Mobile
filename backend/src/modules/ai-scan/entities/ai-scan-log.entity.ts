import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  ManyToOne,
  JoinColumn,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';

@Entity('ai_scan_logs')
export class AiScanLog {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid', nullable: true })
  user_id: string | null;

  @ManyToOne(() => User, { nullable: true, onDelete: 'SET NULL' })
  @JoinColumn({ name: 'user_id' })
  user: User | null;

  @Column({ type: 'text' })
  image_url: string;

  @Column({ type: 'varchar', length: 255, nullable: true })
  image_public_id: string | null;

  @Column({ type: 'text', nullable: true })
  raw_response: string | null;

  @CreateDateColumn()
  created_at: Date;
}
