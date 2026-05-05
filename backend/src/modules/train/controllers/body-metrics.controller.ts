import {
  Controller,
  Get,
  Post,
  Body,
  Query,
  Param,
  Delete,
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
  ApiParam,
} from '@nestjs/swagger';
import { BodyMetricsService } from '../services/body-metrics.service';
import { JwtAuthGuard } from '../../../common/guards/jwt.guard';
import { CurrentUser } from '../../../common/decorators/current-user.decorator';
import type { JwtPayload } from '../../../common/interfaces/jwt-payload.interface';
import { UpsertBodyMetricDto } from '../dto/upsert-body-metric.dto';
import { BodyMetricQueryDto } from '../dto/body-metric-query.dto';

@ApiTags('body-metrics')
@ApiBearerAuth('access-token')
@UseGuards(JwtAuthGuard)
@Controller('body-metrics')
export class BodyMetricsController {
  constructor(private readonly bodyMetricsService: BodyMetricsService) {}

  @ApiOperation({ summary: 'Get body metric history (date / fromDate-toDate)' })
  @ApiQuery({ name: 'date', required: false, example: '2024-01-15' })
  @ApiQuery({ name: 'fromDate', required: false, example: '2024-01-01' })
  @ApiQuery({ name: 'toDate', required: false, example: '2024-01-31' })
  @Get()
  getHistory(@CurrentUser() user: JwtPayload, @Query() query: BodyMetricQueryDto) {
    return this.bodyMetricsService.getHistory(user.sub, query);
  }

  @ApiOperation({ summary: 'Create or update body metric for a date' })
  @Post()
  upsert(@CurrentUser() user: JwtPayload, @Body() dto: UpsertBodyMetricDto) {
    return this.bodyMetricsService.upsert(user.sub, dto);
  }

  @ApiOperation({ summary: 'Get latest body metric' })
  @Get('latest')
  getLatest(@CurrentUser() user: JwtPayload) {
    return this.bodyMetricsService.getLatest(user.sub);
  }

  @ApiOperation({ summary: 'Get weight progress summary' })
  @Get('summary')
  getSummary(@CurrentUser() user: JwtPayload) {
    return this.bodyMetricsService.getProgressSummary(user.sub);
  }

  @ApiOperation({ summary: 'Get body metric history for a date range' })
  @ApiQuery({ name: 'fromDate', required: false, example: '2024-01-01' })
  @ApiQuery({ name: 'toDate', required: false, example: '2024-01-31' })
  @Get('history')
  getHistoryRange(
    @CurrentUser() user: JwtPayload,
    @Query('fromDate') fromDate?: string,
    @Query('toDate') toDate?: string,
  ) {
    const today = new Date().toISOString().split('T')[0];
    return this.bodyMetricsService.getHistory(user.sub, {
      fromDate: fromDate || today,
      toDate: toDate || today,
    });
  }

  @ApiOperation({ summary: 'Get body metrics for a specific date' })
  @Get('history/:date')
  getHistoryByDate(
    @CurrentUser() user: JwtPayload,
    @Param('date') date: string,
  ) {
    return this.bodyMetricsService.getHistory(user.sub, { date });
  }

  @ApiOperation({ summary: 'Get body metrics by time period' })
  @ApiParam({ name: 'period', enum: ['week', 'month', '3months', '6months', 'year'] })
  @Get('period/:period')
  getByPeriod(
    @CurrentUser() user: JwtPayload,
    @Param('period') period: 'week' | 'month' | '3months' | '6months' | 'year',
  ) {
    return this.bodyMetricsService.getByPeriod(user.sub, period);
  }

  @ApiOperation({ summary: 'Get progress photos' })
  @ApiQuery({ name: 'limit', required: false })
  @Get('photos')
  getPhotos(@CurrentUser() user: JwtPayload, @Query('limit') limit?: number) {
    return this.bodyMetricsService.getPhotosByUser(user.sub, limit ? Number(limit) : 10);
  }

  @ApiOperation({ summary: 'Upload a progress photo' })
  @ApiConsumes('multipart/form-data')
  @ApiBody({
    schema: {
      type: 'object',
      properties: {
        file: { type: 'string', format: 'binary' },
        photoType: { type: 'string', enum: ['front', 'back', 'side'], default: 'front' },
        bodyMetricId: { type: 'string', format: 'uuid' },
      },
      required: ['file'],
    },
  })
  @Post('photos')
  @UseInterceptors(FileInterceptor('file', buildMulterOptions('progress-photos')))
  uploadPhoto(
    @CurrentUser() user: JwtPayload,
    @UploadedFile() file: Express.Multer.File,
    @Body('photoType') photoType = 'front',
    @Body('bodyMetricId') bodyMetricId?: string,
  ) {
    return this.bodyMetricsService.uploadPhoto(user.sub, file, photoType, bodyMetricId);
  }

  @ApiOperation({ summary: 'Delete a progress photo' })
  @Delete('photos/:id')
  @HttpCode(HttpStatus.NO_CONTENT)
  deletePhoto(@CurrentUser() user: JwtPayload, @Param('id') id: string) {
    return this.bodyMetricsService.deletePhoto(user.sub, id);
  }
}
