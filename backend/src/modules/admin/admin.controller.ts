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
import {
  ApiTags,
  ApiBearerAuth,
  ApiOperation,
  ApiQuery,
} from '@nestjs/swagger';
import { AdminService } from './admin.service';
import { JwtAuthGuard } from '../../common/guards/jwt.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { Roles } from '../../common/decorators/roles.decorator';
import { UserRole } from '../../common/enums/user-role.enum';
import { CreateFoodAdminDto } from './dto/create-food-admin.dto';
import { UpdateFoodAdminDto } from './dto/update-food-admin.dto';
import { CreateExerciseAdminDto } from './dto/create-exercise-admin.dto';
import { UpdateExerciseAdminDto } from './dto/update-exercise-admin.dto';
import { CreateBlogDto } from './dto/create-blog.dto';
import { UpdateBlogDto } from './dto/update-blog.dto';

@ApiTags('admin')
@ApiBearerAuth('access-token')
@UseGuards(JwtAuthGuard, RolesGuard)
@Roles(UserRole.ADMIN)
@Controller('admin')
export class AdminController {
  constructor(private readonly adminService: AdminService) {}

  // ─── Stats ──────────────────────────────────────────────────────────────────
  @ApiOperation({ summary: 'Get platform stats' })
  @Get('stats')
  getStats() {
    return this.adminService.getStats();
  }

  // ─── Users ──────────────────────────────────────────────────────────────────
  @ApiOperation({ summary: 'List all users' })
  @ApiQuery({ name: 'page', required: false })
  @ApiQuery({ name: 'limit', required: false })
  @ApiQuery({ name: 'search', required: false })
  @Get('users')
  getUsers(
    @Query('page') page?: number,
    @Query('limit') limit?: number,
    @Query('search') search?: string,
  ) {
    return this.adminService.getUsers(
      page ? Number(page) : 1,
      limit ? Number(limit) : 20,
      search,
    );
  }

  @ApiOperation({ summary: 'Get user by ID (with health profile & recent workouts)' })
  @Get('users/:id')
  getUserById(@Param('id') id: string) {
    return this.adminService.getUserById(id);
  }

  @ApiOperation({ summary: 'Ban a user' })
  @Patch('users/:id/ban')
  banUser(@Param('id') id: string) {
    return this.adminService.banUser(id);
  }

  @ApiOperation({ summary: 'Unban a user' })
  @Patch('users/:id/unban')
  unbanUser(@Param('id') id: string) {
    return this.adminService.unbanUser(id);
  }

  // ─── Foods ──────────────────────────────────────────────────────────────────
  @ApiOperation({ summary: 'List all foods (paginated)' })
  @ApiQuery({ name: 'page', required: false })
  @ApiQuery({ name: 'limit', required: false })
  @ApiQuery({ name: 'search', required: false })
  @Get('foods')
  getFoods(
    @Query('page') page?: number,
    @Query('limit') limit?: number,
    @Query('search') search?: string,
  ) {
    return this.adminService.getFoods(
      page ? Number(page) : 1,
      limit ? Number(limit) : 20,
      search,
    );
  }

  @ApiOperation({ summary: 'List foods pending verification' })
  @ApiQuery({ name: 'page', required: false })
  @ApiQuery({ name: 'limit', required: false })
  @Get('foods/pending')
  getPendingFoods(
    @Query('page') page?: number,
    @Query('limit') limit?: number,
  ) {
    return this.adminService.getPendingFoods(
      page ? Number(page) : 1,
      limit ? Number(limit) : 20,
    );
  }

  @ApiOperation({ summary: 'Create a new food entry' })
  @Post('foods')
  createFood(@Body() dto: CreateFoodAdminDto) {
    return this.adminService.createFood(dto);
  }

  @ApiOperation({ summary: 'Update a food entry' })
  @Patch('foods/:id')
  updateFood(@Param('id') id: string, @Body() dto: UpdateFoodAdminDto) {
    return this.adminService.updateFood(id, dto);
  }

  @ApiOperation({ summary: 'Verify (approve) a food entry' })
  @Patch('foods/:id/verify')
  verifyFood(@Param('id') id: string) {
    return this.adminService.verifyFood(id);
  }

  @ApiOperation({ summary: 'Reject a food entry (soft-disable)' })
  @Patch('foods/:id/reject')
  rejectFood(@Param('id') id: string) {
    return this.adminService.rejectFood(id);
  }

  @ApiOperation({ summary: 'Permanently delete a food entry' })
  @Delete('foods/:id')
  @HttpCode(HttpStatus.NO_CONTENT)
  deleteFood(@Param('id') id: string) {
    return this.adminService.deleteFood(id);
  }

  // ─── Exercises ──────────────────────────────────────────────────────────────
  @ApiOperation({ summary: 'List all exercises (paginated)' })
  @ApiQuery({ name: 'page', required: false })
  @ApiQuery({ name: 'limit', required: false })
  @ApiQuery({ name: 'search', required: false })
  @Get('exercises')
  getExercises(
    @Query('page') page?: number,
    @Query('limit') limit?: number,
    @Query('search') search?: string,
  ) {
    return this.adminService.getExercises(
      page ? Number(page) : 1,
      limit ? Number(limit) : 20,
      search,
    );
  }

  @ApiOperation({ summary: 'Create a new exercise' })
  @Post('exercises')
  createExercise(@Body() dto: CreateExerciseAdminDto) {
    return this.adminService.createExercise(dto);
  }

  @ApiOperation({ summary: 'Update an exercise' })
  @Patch('exercises/:id')
  updateExercise(@Param('id') id: string, @Body() dto: UpdateExerciseAdminDto) {
    return this.adminService.updateExercise(id, dto);
  }

  @ApiOperation({ summary: 'Delete an exercise' })
  @Delete('exercises/:id')
  @HttpCode(HttpStatus.NO_CONTENT)
  deleteExercise(@Param('id') id: string) {
    return this.adminService.deleteExercise(id);
  }

  // ─── Blogs ──────────────────────────────────────────────────────────────────
  @ApiOperation({ summary: 'List all blogs (paginated)' })
  @ApiQuery({ name: 'page', required: false })
  @ApiQuery({ name: 'limit', required: false })
  @Get('blogs')
  getBlogs(
    @Query('page') page?: number,
    @Query('limit') limit?: number,
  ) {
    return this.adminService.getBlogs(
      page ? Number(page) : 1,
      limit ? Number(limit) : 20,
    );
  }

  @ApiOperation({ summary: 'Create a new blog post' })
  @Post('blogs')
  createBlog(@Body() dto: CreateBlogDto) {
    return this.adminService.createBlog(dto);
  }

  @ApiOperation({ summary: 'Update a blog post' })
  @Patch('blogs/:id')
  updateBlog(@Param('id') id: string, @Body() dto: UpdateBlogDto) {
    return this.adminService.updateBlog(id, dto);
  }

  @ApiOperation({ summary: 'Delete a blog post' })
  @Delete('blogs/:id')
  @HttpCode(HttpStatus.NO_CONTENT)
  deleteBlog(@Param('id') id: string) {
    return this.adminService.deleteBlog(id);
  }
}
