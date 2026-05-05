import {
  Controller,
  Get,
  Post,
  Patch,
  Delete,
  Body,
  Param,
  Query,
  UseGuards,
  UseInterceptors,
  UploadedFile,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { buildMulterOptions } from '../../../common/utils/multer.config';
import {
  ApiTags,
  ApiBearerAuth,
  ApiOperation,
  ApiConsumes,
  ApiBody,
  ApiQuery,
} from '@nestjs/swagger';
import { TrainingService } from '../services/training.service';
import { JwtAuthGuard } from '../../../common/guards/jwt.guard';
import { CurrentUser } from '../../../common/decorators/current-user.decorator';
import { Public } from '../../../common/decorators/public.decorator';
import type { JwtPayload } from '../../../common/interfaces/jwt-payload.interface';
import {
  ExerciseQueryDto,
  CreateWorkoutSessionDto,
  AddWorkoutDetailDto,
} from '../dto/training.dto';
import { UpdateWorkoutSessionDto } from '../dto/update-training.dto';

@ApiTags('training')
@ApiBearerAuth('access-token')
@UseGuards(JwtAuthGuard)
@Controller('training')
export class TrainingController {
  constructor(private readonly trainingService: TrainingService) {}

  // ─── Exercises ────────────────────────────────────────────────────────────

  @Public()
  @ApiOperation({ summary: 'Get exercises (filter by name, muscleGroup)' })
  @Get('exercises')
  getExercises(@Query() query: ExerciseQueryDto) {
    return this.trainingService.getExercises(query);
  }

  @ApiOperation({ summary: 'Upload avatar image for an exercise' })
  @ApiConsumes('multipart/form-data')
  @ApiBody({
    schema: {
      type: 'object',
      properties: { file: { type: 'string', format: 'binary' } },
      required: ['file'],
    },
  })
  @Post('exercises/:id/image/avatar')
  @UseInterceptors(FileInterceptor('file', buildMulterOptions('exercises')))
  uploadExerciseAvtImage(
    @Param('id') id: string,
    @UploadedFile() file: Express.Multer.File,
  ) {
    return this.trainingService.uploadExerciseAvtImage(id, file);
  }

  @ApiOperation({ summary: 'Add an illustration image to exercise gallery' })
  @ApiConsumes('multipart/form-data')
  @ApiBody({
    schema: {
      type: 'object',
      properties: { file: { type: 'string', format: 'binary' } },
      required: ['file'],
    },
  })
  @Post('exercises/:id/image/gallery')
  @UseInterceptors(FileInterceptor('file', buildMulterOptions('exercises')))
  addExerciseGalleryImage(
    @Param('id') id: string,
    @UploadedFile() file: Express.Multer.File,
  ) {
    return this.trainingService.addExerciseGalleryImage(id, file);
  }

  @ApiOperation({ summary: 'Remove an illustration image from exercise gallery' })
  @Delete('exercises/:id/image/gallery/:publicId')
  removeExerciseGalleryImage(
    @Param('id') id: string,
    @Param('publicId') publicId: string,
  ) {
    return this.trainingService.removeExerciseGalleryImage(id, publicId);
  }

  // ─── Workout Sessions ─────────────────────────────────────────────────────

  @ApiOperation({ summary: 'Create a workout session with one or more exercises' })
  @Post('sessions')
  createWorkoutSession(@CurrentUser() user: JwtPayload, @Body() dto: CreateWorkoutSessionDto) {
    return this.trainingService.createWorkoutSession(user.sub, dto);
  }

  @ApiOperation({ summary: 'Get workout session history (optional date range / limit)' })
  @ApiQuery({ name: 'limit', required: false, example: 20 })
  @ApiQuery({ name: 'fromDate', required: false, example: '2026-01-01' })
  @ApiQuery({ name: 'toDate', required: false, example: '2026-12-31' })
  @Get('sessions')
  getWorkoutHistory(
    @CurrentUser() user: JwtPayload,
    @Query('limit') limit?: string,
    @Query('fromDate') fromDate?: string,
    @Query('toDate') toDate?: string,
  ) {
    if (fromDate || toDate) {
      return this.trainingService.getWorkoutHistoryRange(user.sub, fromDate, toDate);
    }
    return this.trainingService.getWorkoutHistory(user.sub, limit ? Number(limit) : 20);
  }

  @ApiOperation({ summary: 'Get all workout sessions on a specific date' })
  @Get('sessions/date/:date')
  getWorkoutSessionsByDate(@CurrentUser() user: JwtPayload, @Param('date') date: string) {
    return this.trainingService.getWorkoutSessionsByDate(user.sub, date);
  }

  @ApiOperation({ summary: 'Update a workout session (name / notes only)' })
  @Patch('sessions/:id')
  updateWorkoutSession(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @Body() dto: UpdateWorkoutSessionDto,
  ) {
    return this.trainingService.updateWorkoutSession(user.sub, id, dto);
  }

  @ApiOperation({ summary: 'Delete a workout session' })
  @Delete('sessions/:id')
  @HttpCode(HttpStatus.NO_CONTENT)
  deleteWorkoutSession(@CurrentUser() user: JwtPayload, @Param('id') id: string) {
    return this.trainingService.deleteWorkoutSession(user.sub, id);
  }

  @ApiOperation({ summary: 'Add an exercise to an existing workout session' })
  @Post('sessions/:id/exercises')
  addExerciseToSession(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @Body() dto: AddWorkoutDetailDto,
  ) {
    return this.trainingService.addExerciseToSession(user.sub, id, dto);
  }

  @ApiOperation({ summary: 'Remove an exercise from a workout session' })
  @Delete('sessions/:id/exercises/:detailId')
  @HttpCode(HttpStatus.NO_CONTENT)
  removeExerciseFromSession(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @Param('detailId') detailId: string,
  ) {
    return this.trainingService.removeExerciseFromSession(user.sub, id, detailId);
  }

  // ─── Exercise Favorites ───────────────────────────────────────────────────

  @ApiOperation({ summary: 'Get favorite exercises' })
  @Get('exercises/favorites')
  getExerciseFavorites(@CurrentUser() user: JwtPayload) {
    return this.trainingService.getExerciseFavorites(user.sub);
  }

  @ApiOperation({ summary: 'Add exercise to favorites' })
  @Post('exercises/:id/favorite')
  async addExerciseFavorite(@CurrentUser() user: JwtPayload, @Param('id') id: string) {
    await this.trainingService.addExerciseFavorite(user.sub, id);
    return { message: 'Added to favorites' };
  }

  @ApiOperation({ summary: 'Remove exercise from favorites' })
  @Delete('exercises/:id/favorite')
  async removeExerciseFavorite(@CurrentUser() user: JwtPayload, @Param('id') id: string) {
    await this.trainingService.removeExerciseFavorite(user.sub, id);
    return { message: 'Removed from favorites' };
  }
}
