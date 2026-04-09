import { Controller, Get, Param, Query } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiQuery } from '@nestjs/swagger';
import { AdminService } from './admin.service';
import { Public } from '../../common/decorators/public.decorator';

@ApiTags('blogs')
@Controller('blogs')
export class BlogController {
  constructor(private readonly adminService: AdminService) {}

  @Public()
  @ApiOperation({ summary: 'List all blog posts (public)' })
  @ApiQuery({ name: 'page', required: false })
  @ApiQuery({ name: 'limit', required: false })
  @Get()
  getBlogs(
    @Query('page') page?: number,
    @Query('limit') limit?: number,
  ) {
    return this.adminService.getBlogs(
      page ? Number(page) : 1,
      limit ? Number(limit) : 20,
    );
  }

  @Public()
  @ApiOperation({ summary: 'Get a single blog post (public)' })
  @Get(':id')
  getOneBlog(@Param('id') id: string) {
    return this.adminService.getOneBlog(id);
  }
}
