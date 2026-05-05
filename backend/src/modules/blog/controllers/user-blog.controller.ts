import {
  Controller,
  Get,
  Post,
  Patch,
  Delete,
  Param,
  Query,
  Body,
  UseGuards,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth, ApiQuery } from '@nestjs/swagger';
import { BlogService } from '../services/blog.service';
import { JwtAuthGuard } from '../../../common/guards/jwt.guard';
import { CurrentUser } from '../../../common/decorators/current-user.decorator';
import type { JwtPayload } from '../../../common/interfaces/jwt-payload.interface';
import { CreateBlogDto } from '../dto/create-blog.dto';
import { UpdateBlogDto } from '../dto/update-blog.dto';

@ApiTags('user-blogs')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('user/blogs')
export class UserBlogController {
  constructor(private readonly blogService: BlogService) {}

  @ApiOperation({ summary: 'Create a blog post (submitted for approval)' })
  @Post()
  createBlog(@CurrentUser() user: JwtPayload, @Body() dto: CreateBlogDto) {
    return this.blogService.createUserBlog(user.sub, dto);
  }

  @ApiOperation({ summary: 'List my blog posts (all statuses)' })
  @ApiQuery({ name: 'page', required: false })
  @ApiQuery({ name: 'limit', required: false })
  @Get()
  getMyBlogs(
    @CurrentUser() user: JwtPayload,
    @Query('page') page?: string,
    @Query('limit') limit?: string,
  ) {
    return this.blogService.getUserBlogs(
      user.sub,
      page ? Number(page) : 1,
      limit ? Number(limit) : 20,
    );
  }

  @ApiOperation({ summary: 'Update my blog post (re-submits for approval)' })
  @Patch(':id')
  updateBlog(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @Body() dto: UpdateBlogDto,
  ) {
    return this.blogService.updateUserBlog(user.sub, id, dto);
  }

  @ApiOperation({ summary: 'Delete my blog post' })
  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  deleteBlog(@CurrentUser() user: JwtPayload, @Param('id') id: string) {
    return this.blogService.deleteUserBlog(user.sub, id);
  }
}
