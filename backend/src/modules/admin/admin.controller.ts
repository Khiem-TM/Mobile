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
import {
  ApiBearerAuth,
  ApiOperation,
  ApiQuery,
  ApiTags,
} from '@nestjs/swagger';
import type { Request } from 'express';
import { AdminService } from './admin.service';
import { JwtAuthGuard } from '../../common/guards/jwt.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { Roles } from '../../common/decorators/roles.decorator';
import { UserRole } from '../../common/enums/user-role.enum';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtPayload } from '../../common/interfaces/jwt-payload.interface';
import { CreateFoodAdminDto } from './dto/create-food-admin.dto';
import { UpdateFoodAdminDto } from './dto/update-food-admin.dto';
import { CreateExerciseAdminDto } from './dto/create-exercise-admin.dto';
import { UpdateExerciseAdminDto } from './dto/update-exercise-admin.dto';
import { CreateUserAdminDto } from './dto/create-user-admin.dto';
import { UpdateUserAdminDto } from './dto/update-user-admin.dto';
import { AdminWarningDto } from './dto/admin-warning.dto';
import { AdminAuditContext } from './services/audit-log.service';

@ApiTags('admin')
@ApiBearerAuth('access-token')
@UseGuards(JwtAuthGuard, RolesGuard)
@Roles(UserRole.ADMIN)
@Controller('admin')
export class AdminController {
  constructor(private readonly adminService: AdminService) {}

  @ApiOperation({ summary: 'Get platform stats' })
  @Get('stats')
  getStats() {
    return this.adminService.getStats();
  }

  @ApiOperation({ summary: 'Get platform health status' })
  @Get('health')
  getHealth() {
    return this.adminService.getHealth();
  }

  @ApiOperation({ summary: 'Get admin dashboard overview analytics' })
  @Get('analytics/overview')
  getAnalyticsOverview(
    @Query('fromDate') fromDate?: string,
    @Query('toDate') toDate?: string,
    @Query('granularity') granularity?: string,
  ) {
    return this.adminService.getAnalyticsOverview(fromDate, toDate, granularity);
  }

  @ApiOperation({ summary: 'Get user analytics' })
  @Get('analytics/users')
  getUserAnalytics(
    @Query('fromDate') fromDate?: string,
    @Query('toDate') toDate?: string,
    @Query('granularity') granularity?: string,
  ) {
    return this.adminService.getUserAnalytics(fromDate, toDate, granularity);
  }

  @ApiOperation({ summary: 'Get nutrition analytics' })
  @Get('analytics/nutrition')
  getNutritionAnalytics(
    @Query('fromDate') fromDate?: string,
    @Query('toDate') toDate?: string,
    @Query('granularity') granularity?: string,
  ) {
    return this.adminService.getNutritionAnalytics(fromDate, toDate, granularity);
  }

  @ApiOperation({ summary: 'Get training analytics' })
  @Get('analytics/training')
  getTrainingAnalytics(
    @Query('fromDate') fromDate?: string,
    @Query('toDate') toDate?: string,
    @Query('granularity') granularity?: string,
  ) {
    return this.adminService.getTrainingAnalytics(fromDate, toDate, granularity);
  }

  @ApiOperation({ summary: 'Get blog analytics' })
  @Get('analytics/blogs')
  getBlogAnalytics(
    @Query('fromDate') fromDate?: string,
    @Query('toDate') toDate?: string,
    @Query('granularity') granularity?: string,
  ) {
    return this.adminService.getBlogAnalytics(fromDate, toDate, granularity);
  }

  @ApiOperation({ summary: 'Get AI usage analytics' })
  @Get('analytics/ai')
  getAiAnalytics(
    @Query('fromDate') fromDate?: string,
    @Query('toDate') toDate?: string,
    @Query('granularity') granularity?: string,
  ) {
    return this.adminService.getAiAnalytics(fromDate, toDate, granularity);
  }

  @ApiOperation({ summary: 'List all users' })
  @ApiQuery({ name: 'page', required: false })
  @ApiQuery({ name: 'limit', required: false })
  @ApiQuery({ name: 'search', required: false })
  @ApiQuery({ name: 'role', required: false })
  @ApiQuery({ name: 'isActive', required: false })
  @ApiQuery({ name: 'isVerified', required: false })
  @ApiQuery({ name: 'createdFrom', required: false })
  @ApiQuery({ name: 'createdTo', required: false })
  @Get('users')
  getUsers(
    @Query('page') page?: string,
    @Query('limit') limit?: string,
    @Query('search') search?: string,
    @Query('role') role?: string,
    @Query('isActive') isActive?: string,
    @Query('isVerified') isVerified?: string,
    @Query('createdFrom') createdFrom?: string,
    @Query('createdTo') createdTo?: string,
  ) {
    return this.adminService.getUsers(
      page ? Number(page) : 1,
      limit ? Number(limit) : 20,
      search,
      { role, isActive, isVerified, createdFrom, createdTo },
    );
  }

  @ApiOperation({ summary: 'Create a user' })
  @Post('users')
  createUser(
    @Body() dto: CreateUserAdminDto,
    @CurrentUser() user: JwtPayload,
    @Req() req: Request,
  ) {
    return this.adminService.createUser(dto, this.auditContext(user, req));
  }

  @ApiOperation({
    summary: 'Get user by ID (with health profile & recent workouts)',
  })
  @Get('users/:id')
  getUserById(@Param('id') id: string) {
    return this.adminService.getUserById(id);
  }

  @ApiOperation({ summary: 'Update a user' })
  @Patch('users/:id')
  updateUser(
    @Param('id') id: string,
    @Body() dto: UpdateUserAdminDto,
    @CurrentUser() user: JwtPayload,
    @Req() req: Request,
  ) {
    return this.adminService.updateUser(id, dto, this.auditContext(user, req));
  }

  @ApiOperation({ summary: 'Ban a user' })
  @Patch('users/:id/ban')
  banUser(
    @Param('id') id: string,
    @CurrentUser() user: JwtPayload,
    @Req() req: Request,
  ) {
    return this.adminService.banUser(id, this.auditContext(user, req));
  }

  @ApiOperation({ summary: 'Unban a user' })
  @Patch('users/:id/unban')
  unbanUser(
    @Param('id') id: string,
    @CurrentUser() user: JwtPayload,
    @Req() req: Request,
  ) {
    return this.adminService.unbanUser(id, this.auditContext(user, req));
  }

  @ApiOperation({ summary: 'Force-verify a user email' })
  @Patch('users/:id/verify-email')
  forceVerifyEmail(
    @Param('id') id: string,
    @CurrentUser() user: JwtPayload,
    @Req() req: Request,
  ) {
    return this.adminService.forceVerifyEmail(id, this.auditContext(user, req));
  }

  @ApiOperation({ summary: 'Send a warning notification to a user' })
  @Post('users/:id/warnings')
  warnUser(
    @Param('id') id: string,
    @Body() dto: AdminWarningDto,
    @CurrentUser() user: JwtPayload,
    @Req() req: Request,
  ) {
    return this.adminService.warnUser(id, dto, this.auditContext(user, req));
  }

  @ApiOperation({ summary: 'List all foods (paginated)' })
  @ApiQuery({ name: 'page', required: false })
  @ApiQuery({ name: 'limit', required: false })
  @ApiQuery({ name: 'search', required: false })
  @ApiQuery({ name: 'foodType', required: false })
  @ApiQuery({ name: 'category', required: false })
  @ApiQuery({ name: 'isVerified', required: false })
  @ApiQuery({ name: 'isActive', required: false })
  @ApiQuery({ name: 'createdFrom', required: false })
  @ApiQuery({ name: 'createdTo', required: false })
  @Get('foods')
  getFoods(
    @Query('page') page?: string,
    @Query('limit') limit?: string,
    @Query('search') search?: string,
    @Query('foodType') foodType?: string,
    @Query('category') category?: string,
    @Query('isVerified') isVerified?: string,
    @Query('isActive') isActive?: string,
    @Query('createdFrom') createdFrom?: string,
    @Query('createdTo') createdTo?: string,
  ) {
    return this.adminService.getFoods(
      page ? Number(page) : 1,
      limit ? Number(limit) : 20,
      search,
      { foodType, category, isVerified, isActive, createdFrom, createdTo },
    );
  }

  @ApiOperation({ summary: 'List foods pending verification' })
  @ApiQuery({ name: 'page', required: false })
  @ApiQuery({ name: 'limit', required: false })
  @Get('foods/pending')
  getPendingFoods(
    @Query('page') page?: string,
    @Query('limit') limit?: string,
  ) {
    return this.adminService.getPendingFoods(
      page ? Number(page) : 1,
      limit ? Number(limit) : 20,
    );
  }

  @ApiOperation({ summary: 'Create a new food entry' })
  @Post('foods')
  createFood(
    @Body() dto: CreateFoodAdminDto,
    @CurrentUser() user: JwtPayload,
    @Req() req: Request,
  ) {
    return this.adminService.createFood(dto, this.auditContext(user, req));
  }

  @ApiOperation({ summary: 'Update a food entry' })
  @Patch('foods/:id')
  updateFood(
    @Param('id') id: string,
    @Body() dto: UpdateFoodAdminDto,
    @CurrentUser() user: JwtPayload,
    @Req() req: Request,
  ) {
    return this.adminService.updateFood(id, dto, this.auditContext(user, req));
  }

  @ApiOperation({ summary: 'Verify (approve) a food entry' })
  @Patch('foods/:id/verify')
  verifyFood(
    @Param('id') id: string,
    @CurrentUser() user: JwtPayload,
    @Req() req: Request,
  ) {
    return this.adminService.verifyFood(id, this.auditContext(user, req));
  }

  @ApiOperation({ summary: 'Reject a food entry (soft-disable)' })
  @Patch('foods/:id/reject')
  rejectFood(
    @Param('id') id: string,
    @CurrentUser() user: JwtPayload,
    @Req() req: Request,
  ) {
    return this.adminService.rejectFood(id, this.auditContext(user, req));
  }

  @ApiOperation({ summary: 'Soft-delete a food entry' })
  @Delete('foods/:id')
  @HttpCode(HttpStatus.NO_CONTENT)
  deleteFood(
    @Param('id') id: string,
    @CurrentUser() user: JwtPayload,
    @Req() req: Request,
  ) {
    return this.adminService.deleteFood(id, this.auditContext(user, req));
  }

  @ApiOperation({ summary: 'List all exercises (paginated)' })
  @ApiQuery({ name: 'page', required: false })
  @ApiQuery({ name: 'limit', required: false })
  @ApiQuery({ name: 'search', required: false })
  @ApiQuery({ name: 'exerciseType', required: false })
  @ApiQuery({ name: 'category', required: false })
  @ApiQuery({ name: 'muscleGroup', required: false })
  @ApiQuery({ name: 'difficultyLevel', required: false })
  @ApiQuery({ name: 'isActive', required: false })
  @Get('exercises')
  getExercises(
    @Query('page') page?: string,
    @Query('limit') limit?: string,
    @Query('search') search?: string,
    @Query('exerciseType') exerciseType?: string,
    @Query('category') category?: string,
    @Query('muscleGroup') muscleGroup?: string,
    @Query('difficultyLevel') difficultyLevel?: string,
    @Query('isActive') isActive?: string,
  ) {
    return this.adminService.getExercises(
      page ? Number(page) : 1,
      limit ? Number(limit) : 20,
      search,
      { exerciseType, category, muscleGroup, difficultyLevel, isActive },
    );
  }

  @ApiOperation({ summary: 'Create a new exercise' })
  @Post('exercises')
  createExercise(
    @Body() dto: CreateExerciseAdminDto,
    @CurrentUser() user: JwtPayload,
    @Req() req: Request,
  ) {
    return this.adminService.createExercise(dto, this.auditContext(user, req));
  }

  @ApiOperation({ summary: 'Update an exercise' })
  @Patch('exercises/:id')
  updateExercise(
    @Param('id') id: string,
    @Body() dto: UpdateExerciseAdminDto,
    @CurrentUser() user: JwtPayload,
    @Req() req: Request,
  ) {
    return this.adminService.updateExercise(id, dto, this.auditContext(user, req));
  }

  @ApiOperation({ summary: 'Soft-delete an exercise' })
  @Delete('exercises/:id')
  @HttpCode(HttpStatus.NO_CONTENT)
  deleteExercise(
    @Param('id') id: string,
    @CurrentUser() user: JwtPayload,
    @Req() req: Request,
  ) {
    return this.adminService.deleteExercise(id, this.auditContext(user, req));
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
