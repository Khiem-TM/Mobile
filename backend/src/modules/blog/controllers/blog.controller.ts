import {
  Controller,
  Get,
  Post,
  Delete,
  Param,
  Query,
  Body,
  UseGuards,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import { ApiTags, ApiOperation, ApiQuery, ApiBearerAuth } from '@nestjs/swagger';
import { BlogService } from '../services/blog.service';
import { Public } from '../../../common/decorators/public.decorator';
import { JwtAuthGuard } from '../../../common/guards/jwt.guard';
import { CurrentUser } from '../../../common/decorators/current-user.decorator';
import { UserRole } from '../../../common/enums/user-role.enum';
import type { JwtPayload } from '../../../common/interfaces/jwt-payload.interface';
import { CreateCommentDto } from '../dto/create-comment.dto';

@ApiTags('blogs')
@Controller('blogs')
export class BlogController {
  constructor(private readonly blogService: BlogService) {}

  @Public()
  @ApiOperation({ summary: 'List approved blog posts (public)' })
  @ApiQuery({ name: 'page', required: false })
  @ApiQuery({ name: 'limit', required: false })
  @ApiQuery({ name: 'search', required: false })
  @ApiQuery({ name: 'tag', required: false })
  @Get()
  getBlogs(
    @Query('page') page?: string,
    @Query('limit') limit?: string,
    @Query('search') search?: string,
    @Query('tag') tag?: string,
  ) {
    return this.blogService.getApprovedBlogs(
      page ? Number(page) : 1,
      limit ? Number(limit) : 20,
      search,
      tag,
    );
  }

  // Feature 2: must be BEFORE :id to avoid param swallowing
  @Public()
  @ApiOperation({ summary: 'List all unique tags from approved blogs (public)' })
  @Get('tags')
  getAllTags() {
    return this.blogService.getAllTags();
  }

  @Public()
  @ApiOperation({ summary: 'Get a single approved blog post (public)' })
  @Get(':id')
  getOneBlog(@Param('id') id: string) {
    return this.blogService.getOneBlog(id);
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @ApiOperation({ summary: 'Toggle like on a blog post' })
  @Post(':id/like')
  toggleLike(@Param('id') id: string, @CurrentUser() user: JwtPayload) {
    return this.blogService.toggleLike(user.sub, id);
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @ApiOperation({ summary: 'Check if current user liked a blog post' })
  @Get(':id/liked')
  isLiked(@Param('id') id: string, @CurrentUser() user: JwtPayload) {
    return this.blogService.isLiked(user.sub, id);
  }

  // ─── Feature 1: Comments ──────────────────────────────────────────────────

  @Public()
  @ApiOperation({ summary: 'Get comments for a blog post (public)' })
  @ApiQuery({ name: 'page', required: false })
  @ApiQuery({ name: 'limit', required: false })
  @Get(':id/comments')
  getComments(
    @Param('id') id: string,
    @Query('page') page?: string,
    @Query('limit') limit?: string,
  ) {
    return this.blogService.getComments(
      id,
      page ? Number(page) : 1,
      limit ? Number(limit) : 20,
    );
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @ApiOperation({ summary: 'Add a comment to a blog post (authenticated)' })
  @Post(':id/comments')
  addComment(
    @Param('id') id: string,
    @CurrentUser() user: JwtPayload,
    @Body() dto: CreateCommentDto,
  ) {
    return this.blogService.addComment(user.sub, id, dto);
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @ApiOperation({ summary: 'Delete a comment (owner or admin)' })
  @Delete(':id/comments/:commentId')
  @HttpCode(HttpStatus.NO_CONTENT)
  async deleteComment(
    @Param('commentId') commentId: string,
    @CurrentUser() user: JwtPayload,
  ) {
    if (user.role === UserRole.ADMIN) {
      return this.blogService.adminDeleteComment(commentId);
    }
    return this.blogService.deleteOwnComment(user.sub, commentId);
  }
}
