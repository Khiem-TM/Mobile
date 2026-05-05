import {
  Entity,
  CreateDateColumn,
  ManyToOne,
  JoinColumn,
  PrimaryColumn,
} from 'typeorm';
import { User } from '../../user/entities/user.entity';
import { Blog } from './blog.entity';

@Entity('blog_likes')
export class BlogLike {
  @PrimaryColumn('uuid')
  user_id!: string;

  @PrimaryColumn('uuid')
  blog_id!: string;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user!: User;

  @ManyToOne(() => Blog, (blog) => blog.likes, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'blog_id' })
  blog!: Blog;

  @CreateDateColumn({ name: 'created_at' })
  createdAt!: Date;
}
