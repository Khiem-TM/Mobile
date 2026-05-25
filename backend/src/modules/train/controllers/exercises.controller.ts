import {
  Controller,
  Get,
  Post,
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
} from '@nestjs/swagger';
import { ExercisesService } from '../services/exercises.service';
import { JwtAuthGuard } from '../../../common/guards/jwt.guard';
import { CurrentUser } from '../../../common/decorators/current-user.decorator';
import { Public } from '../../../common/decorators/public.decorator';
import type { JwtPayload } from '../../../common/interfaces/jwt-payload.interface';
import { ExerciseQueryDto } from '../dto/exercise-query.dto';

@ApiTags('exercises')
@ApiBearerAuth('access-token')
@UseGuards(JwtAuthGuard)
@Controller('exercises')
export class ExercisesController {
  constructor(private readonly exercisesService: ExercisesService) {}

  @Public()
  @ApiOperation({ summary: 'Get exercises (filter by name, type, category, muscleGroup, difficulty)' })
  @Get()
  getExercises(@Query() query: ExerciseQueryDto, @CurrentUser() user?: JwtPayload) {
    return this.exercisesService.getExercises(query, user?.sub);
  }

  @Public()
  @ApiOperation({ summary: 'Get popular exercises by favorites count' })
  @Get('popular')
  getPopular(@Query('limit') limit?: string, @CurrentUser() user?: JwtPayload) {
    return this.exercisesService.getPopularExercises(limit ? Number(limit) : 10, user?.sub);
  }

  @Public()
  @ApiOperation({ summary: 'Get exercise by ID' })
  @Get(':id')
  getById(@Param('id') id: string, @CurrentUser() user?: JwtPayload) {
    return this.exercisesService.getExerciseById(id, user?.sub);
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
  @Post(':id/image/avatar')
  @UseInterceptors(FileInterceptor('file', buildMulterOptions('exercises')))
  uploadAvtImage(
    @Param('id') id: string,
    @UploadedFile() file: Express.Multer.File,
  ) {
    return this.exercisesService.uploadExerciseAvtImage(id, file);
  }

  @ApiOperation({ summary: 'Add image to exercise gallery' })
  @ApiConsumes('multipart/form-data')
  @ApiBody({
    schema: {
      type: 'object',
      properties: { file: { type: 'string', format: 'binary' } },
      required: ['file'],
    },
  })
  @Post(':id/image/gallery')
  @UseInterceptors(FileInterceptor('file', buildMulterOptions('exercises')))
  addGalleryImage(
    @Param('id') id: string,
    @UploadedFile() file: Express.Multer.File,
  ) {
    return this.exercisesService.addExerciseGalleryImage(id, file);
  }

  @ApiOperation({ summary: 'Remove image from exercise gallery' })
  @Delete(':id/image/gallery/:publicId')
  @HttpCode(HttpStatus.NO_CONTENT)
  removeGalleryImage(@Param('id') id: string, @Param('publicId') publicId: string) {
    return this.exercisesService.removeExerciseGalleryImage(id, publicId);
  }
}
