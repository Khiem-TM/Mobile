import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { SupportModule } from '../support/support.module';
import { Blog } from './entities/blog.entity';
import { BlogBlock } from './entities/blog-block.entity';
import { BlogLike } from './entities/blog-like.entity';
import { BlogComment } from './entities/blog-comment.entity';
import { BlogController } from './controllers/blog.controller';
import { UserBlogController } from './controllers/user-blog.controller';
import { AdminBlogController } from './controllers/admin-blog.controller';
import { BlogService } from './services/blog.service';

@Module({
  imports: [
    TypeOrmModule.forFeature([Blog, BlogBlock, BlogLike, BlogComment]),
    SupportModule,
  ],
  controllers: [BlogController, UserBlogController, AdminBlogController],
  providers: [BlogService],
  exports: [BlogService],
})
export class BlogModule {}
