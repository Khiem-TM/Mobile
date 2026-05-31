import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ClientsModule, Transport } from '@nestjs/microservices';
import { SupportModule } from '../support/support.module';
import { UserModule } from '../user/user.module';
import { Blog } from './entities/blog.entity';
import { BlogBlock } from './entities/blog-block.entity';
import { BlogLike } from './entities/blog-like.entity';
import { BlogComment } from './entities/blog-comment.entity';
import { BlogController } from './controllers/blog.controller';
import { UserBlogController } from './controllers/user-blog.controller';
import { AdminBlogController } from './controllers/admin-blog.controller';
import { BlogService } from './services/blog.service';
import { BlogEventPublisher, KAFKA_CLIENT } from './services/blog-event.publisher';

@Module({
  imports: [
    TypeOrmModule.forFeature([Blog, BlogBlock, BlogLike, BlogComment]),
    SupportModule,
    UserModule, // UsersService -> lấy tên người like/comment
    ClientsModule.register([
      {
        name: KAFKA_CLIENT,
        transport: Transport.KAFKA,
        options: {
          client: {
            clientId: process.env.KAFKA_CLIENT_ID || 'vitalai-backend',
            brokers: (process.env.KAFKA_BROKERS || 'localhost:9094').split(','),
          },
          producerOnlyMode: true,
        },
      },
    ]),
  ],
  controllers: [BlogController, UserBlogController, AdminBlogController],
  providers: [BlogService, BlogEventPublisher],
  exports: [BlogService],
})
export class BlogModule {}
