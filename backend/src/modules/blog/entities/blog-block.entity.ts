import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  ManyToOne,
  JoinColumn,
} from 'typeorm';
import { Blog } from './blog.entity';

export type BlogBlockType = 'text' | 'image';

@Entity('blog_blocks')
export class BlogBlock {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ type: 'uuid', name: 'blog_id' })
  blog_id!: string;

  @ManyToOne(() => Blog, (blog) => blog.blocks, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'blog_id' })
  blog!: Blog;

  @Column({ type: 'int' })
  order!: number;

  @Column({ type: 'varchar', length: 10 })
  type!: BlogBlockType;

  @Column({ type: 'text', nullable: true, name: 'text_content' })
  textContent!: string | null;

  @Column({ type: 'text', nullable: true, name: 'image_url' })
  imageUrl!: string | null;

  @Column({ type: 'varchar', length: 255, nullable: true, name: 'image_public_id' })
  imagePublicId!: string | null;

  @CreateDateColumn({ name: 'created_at' })
  createdAt!: Date;
}
