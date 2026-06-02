import {
  Body,
  Controller,
  Delete,
  Get,
  HttpCode,
  HttpStatus,
  Param,
  Patch,
  Post,
  Query,
  Req,
  UseGuards,
} from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiQuery, ApiTags } from '@nestjs/swagger';
import type { Request } from 'express';
import { BlogService } from '../services/blog.service';
import { JwtAuthGuard } from '../../../common/guards/jwt.guard';
import { RolesGuard } from '../../../common/guards/roles.guard';
import { Roles } from '../../../common/decorators/roles.decorator';
import { UserRole } from '../../../common/enums/user-role.enum';
import { CurrentUser } from '../../../common/decorators/current-user.decorator';
import type { JwtPayload } from '../../../common/interfaces/jwt-payload.interface';
import { CreateBlogDto } from '../dto/create-blog.dto';
import { UpdateBlogDto } from '../dto/update-blog.dto';
import { RejectBlogDto } from '../dto/reject-blog.dto';
import { BatchBlogActionDto, BatchRejectBlogDto } from '../dto/batch-blog.dto';
import { AdminAuditContext } from '../../admin/services/audit-log.service';

@ApiTags('admin-blogs')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard, RolesGuard)
@Roles(UserRole.ADMIN)
@Controller('admin/blogs')
export class AdminBlogController {
  constructor(private readonly blogService: BlogService) {}

  @ApiOperation({ summary: 'List all blog posts (filterable by status, tag, author, dates, search)' })
  @ApiQuery({ name: 'page', required: false })
  @ApiQuery({ name: 'limit', required: false })
  @ApiQuery({ name: 'status', required: false, enum: ['pending', 'approved', 'rejected', 'draft'] })
  @ApiQuery({ name: 'tag', required: false })
  @ApiQuery({ name: 'search', required: false })
  @ApiQuery({ name: 'authorId', required: false })
  @ApiQuery({ name: 'createdFrom', required: false })
  @ApiQuery({ name: 'createdTo', required: false })
  @Get()
  getBlogs(
    @Query('page') page?: string,
    @Query('limit') limit?: string,
    @Query('status') status?: string,
    @Query('tag') tag?: string,
    @Query('search') search?: string,
    @Query('authorId') authorId?: string,
    @Query('createdFrom') createdFrom?: string,
    @Query('createdTo') createdTo?: string,
  ) {
    return this.blogService.adminGetBlogs(
      page ? Number(page) : 1,
      limit ? Number(limit) : 20,
      { status, tag, search, authorId, createdFrom, createdTo },
    );
  }

  @ApiOperation({ summary: 'Get a blog post by ID for moderation' })
  @Get(':id')
  getBlogById(@Param('id') id: string) {
    return this.blogService.adminGetBlogById(id);
  }

  @ApiOperation({ summary: 'List comments for a blog post for moderation' })
  @ApiQuery({ name: 'page', required: false })
  @ApiQuery({ name: 'limit', required: false })
  @Get(':id/comments')
  getBlogComments(
    @Param('id') id: string,
    @Query('page') page?: string,
    @Query('limit') limit?: string,
  ) {
    return this.blogService.adminGetBlogComments(
      id,
      page ? Number(page) : 1,
      limit ? Number(limit) : 20,
    );
  }

  @ApiOperation({ summary: 'Create a blog post (published immediately)' })
  @Post()
  createBlog(
    @Body() dto: CreateBlogDto,
    @CurrentUser() user: JwtPayload,
    @Req() req: Request,
  ) {
    return this.blogService.adminCreateBlog(dto, this.auditContext(user, req));
  }

  @ApiOperation({ summary: 'Batch approve blog posts' })
  @Post('batch/approve')
  batchApprove(
    @Body() dto: BatchBlogActionDto,
    @CurrentUser() user: JwtPayload,
    @Req() req: Request,
  ) {
    return this.blogService.adminBatchApproveBlogs(dto.ids, this.auditContext(user, req));
  }

  @ApiOperation({ summary: 'Batch reject blog posts' })
  @Post('batch/reject')
  batchReject(
    @Body() dto: BatchRejectBlogDto,
    @CurrentUser() user: JwtPayload,
    @Req() req: Request,
  ) {
    return this.blogService.adminBatchRejectBlogs(
      dto.ids,
      dto.reason,
      this.auditContext(user, req),
    );
  }

  @ApiOperation({ summary: 'Approve a blog post' })
  @Patch(':id/approve')
  approveBlog(
    @Param('id') id: string,
    @CurrentUser() user: JwtPayload,
    @Req() req: Request,
  ) {
    return this.blogService.adminApproveBlog(id, this.auditContext(user, req));
  }

  @ApiOperation({ summary: 'Reject a blog post' })
  @Patch(':id/reject')
  rejectBlog(
    @Param('id') id: string,
    @Body() dto: RejectBlogDto,
    @CurrentUser() user: JwtPayload,
    @Req() req: Request,
  ) {
    return this.blogService.adminRejectBlog(id, dto.reason, this.auditContext(user, req));
  }

  @ApiOperation({ summary: 'Update any blog post' })
  @Patch(':id')
  updateBlog(
    @Param('id') id: string,
    @Body() dto: UpdateBlogDto,
    @CurrentUser() user: JwtPayload,
    @Req() req: Request,
  ) {
    return this.blogService.adminUpdateBlog(id, dto, this.auditContext(user, req));
  }

  @ApiOperation({ summary: 'Soft-delete any blog post' })
  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  deleteBlog(
    @Param('id') id: string,
    @CurrentUser() user: JwtPayload,
    @Req() req: Request,
  ) {
    return this.blogService.adminDeleteBlog(id, this.auditContext(user, req));
  }

  @ApiOperation({ summary: 'Soft-delete a blog comment' })
  @Delete(':id/comments/:commentId')
  @HttpCode(HttpStatus.NO_CONTENT)
  deleteComment(
    @Param('id') id: string,
    @Param('commentId') commentId: string,
    @CurrentUser() user: JwtPayload,
    @Req() req: Request,
  ) {
    return this.blogService.adminDeleteComment(
      commentId,
      this.auditContext(user, req),
      id,
    );
  }

  private auditContext(user: JwtPayload, req: Request): AdminAuditContext {
    return {
      actorUserId: user.sub,
      actorEmail: user.email,
      ipAddress: req.ip,
      userAgent: req.get('user-agent') ?? null,
    };
  }
}
