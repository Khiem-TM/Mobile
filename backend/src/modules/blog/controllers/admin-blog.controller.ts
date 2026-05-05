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
import { RolesGuard } from '../../../common/guards/roles.guard';
import { Roles } from '../../../common/decorators/roles.decorator';
import { UserRole } from '../../../common/enums/user-role.enum';
import { CreateBlogDto } from '../dto/create-blog.dto';
import { UpdateBlogDto } from '../dto/update-blog.dto';
import { RejectBlogDto } from '../dto/reject-blog.dto';
import { BatchBlogActionDto, BatchRejectBlogDto } from '../dto/batch-blog.dto';

@ApiTags('admin-blogs')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard, RolesGuard)
@Roles(UserRole.ADMIN)
@Controller('admin/blogs')
export class AdminBlogController {
  constructor(private readonly blogService: BlogService) {}

  @ApiOperation({ summary: 'List all blog posts (filterable by status and tag)' })
  @ApiQuery({ name: 'page', required: false })
  @ApiQuery({ name: 'limit', required: false })
  @ApiQuery({ name: 'status', required: false, enum: ['pending', 'approved', 'rejected', 'draft'] })
  @ApiQuery({ name: 'tag', required: false })
  @Get()
  getBlogs(
    @Query('page') page?: string,
    @Query('limit') limit?: string,
    @Query('status') status?: string,
    @Query('tag') tag?: string,
  ) {
    return this.blogService.adminGetBlogs(
      page ? Number(page) : 1,
      limit ? Number(limit) : 20,
      status,
      tag,
    );
  }

  @ApiOperation({ summary: 'Get pending blogs count' })
  @Get('pending-count')
  getPendingCount() {
    return this.blogService.getPendingCount();
  }

  // Feature 5: batch endpoints — MUST be declared before @Patch(':id')
  @ApiOperation({ summary: 'Batch approve blog posts' })
  @Patch('batch-approve')
  batchApprove(@Body() dto: BatchBlogActionDto) {
    return this.blogService.adminBatchApprove(dto);
  }

  @ApiOperation({ summary: 'Batch reject blog posts' })
  @Patch('batch-reject')
  batchReject(@Body() dto: BatchRejectBlogDto) {
    return this.blogService.adminBatchReject(dto);
  }

  @ApiOperation({ summary: 'Create a blog post (published immediately)' })
  @Post()
  createBlog(@Body() dto: CreateBlogDto) {
    return this.blogService.adminCreateBlog(dto);
  }

  @ApiOperation({ summary: 'Update any blog post' })
  @Patch(':id')
  updateBlog(@Param('id') id: string, @Body() dto: UpdateBlogDto) {
    return this.blogService.adminUpdateBlog(id, dto);
  }

  @ApiOperation({ summary: 'Approve a blog post' })
  @Patch(':id/approve')
  approveBlog(@Param('id') id: string) {
    return this.blogService.adminApproveBlog(id);
  }

  @ApiOperation({ summary: 'Reject a blog post' })
  @Patch(':id/reject')
  rejectBlog(@Param('id') id: string, @Body() dto: RejectBlogDto) {
    return this.blogService.adminRejectBlog(id, dto);
  }

  @ApiOperation({ summary: 'Delete any blog post' })
  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  deleteBlog(@Param('id') id: string) {
    return this.blogService.adminDeleteBlog(id);
  }
}
