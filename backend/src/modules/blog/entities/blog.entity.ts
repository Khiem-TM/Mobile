import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
  ManyToOne,
  OneToMany,
  JoinColumn,
} from 'typeorm';
import { User } from '../../user/entities/user.entity';
import { BlogBlock } from './blog-block.entity';
import { BlogLike } from './blog-like.entity';
import { BlogComment } from './blog-comment.entity';

export type BlogStatus = 'pending' | 'approved' | 'rejected' | 'draft';

@Entity('blogs')
export class Blog {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ type: 'varchar', length: 255 })
  title!: string;

  @Column({ type: 'varchar', length: 100, nullable: true })
  author!: string | null;

  // Legacy field — kept nullable so existing rows don't violate NOT NULL
  @Column({ type: 'text', nullable: true })
  content!: string | null;

  @Column({ type: 'uuid', nullable: true, name: 'author_id' })
  author_id!: string | null;

  @ManyToOne(() => User, { nullable: true, onDelete: 'SET NULL' })
  @JoinColumn({ name: 'author_id' })
  authorUser!: User | null;

  @Column({ name: 'thumbnail_url', type: 'text', nullable: true })
  thumbnailUrl!: string | null;

  @Column({ name: 'thumbnail_public_id', type: 'varchar', length: 255, nullable: true })
  thumbnailPublicId!: string | null;

  @Column({
    type: 'varchar',
    length: 20,
    default: 'approved',
  })
  status!: BlogStatus;

  @Column({ name: 'rejection_reason', type: 'text', nullable: true })
  rejectionReason!: string | null;

  @Column({ name: 'likes_count', type: 'int', default: 0 })
  likesCount!: number;

  @Column({ name: 'view_count', type: 'int', default: 0 })
  viewCount!: number;

  @Column({ name: 'comment_count', type: 'int', default: 0 })
  commentCount!: number;

  @Column({ type: 'simple-array', nullable: true, default: null })
  tags!: string[] | null;

  @OneToMany(() => BlogBlock, (block) => block.blog, { cascade: true })
  blocks!: BlogBlock[];

  @OneToMany(() => BlogLike, (like) => like.blog)
  likes!: BlogLike[];

  @OneToMany(() => BlogComment, (comment) => comment.blog)
  comments!: BlogComment[];

  @CreateDateColumn({ name: 'created_at' })
  createdAt!: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt!: Date;
}
