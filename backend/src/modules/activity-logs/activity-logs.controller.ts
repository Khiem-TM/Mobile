import { Controller, Get, Patch, Body, Query, UseGuards } from '@nestjs/common';
import {
  ApiTags,
  ApiBearerAuth,
  ApiOperation,
  ApiQuery,
} from '@nestjs/swagger';
import { ActivityLogsService } from './activity-logs.service';
import { JwtAuthGuard } from '../../common/guards/jwt.guard';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtPayload } from '../../common/interfaces/jwt-payload.interface';
import { LogStepsDto } from './dto/log-steps.dto';
import { LogCaloriesBurnedDto } from './dto/log-calories-burned.dto';
import { LogWaterDto } from './dto/log-water.dto';
import { ActivityLogQueryDto } from './dto/activity-log-query.dto';

@ApiTags('activity-logs')
@ApiBearerAuth('access-token')
@Controller('activity-logs')
@UseGuards(JwtAuthGuard)
export class ActivityLogsController {
  constructor(private readonly activityLogsService: ActivityLogsService) {}

  @ApiOperation({ summary: 'Get activity log for a specific date' })
  @ApiQuery({ name: 'date', required: false, example: '2024-01-15' })
  @Get()
  async getByDate(
    @CurrentUser() user: JwtPayload,
    @Query() query: ActivityLogQueryDto,
  ) {
    const date = query.date || new Date().toISOString().split('T')[0];
    return this.activityLogsService.getByDate(user.sub, date);
  }

  @ApiOperation({ summary: 'Get activity logs for a date range' })
  @ApiQuery({ name: 'fromDate', required: false, example: '2024-01-01' })
  @ApiQuery({ name: 'toDate', required: false, example: '2024-01-07' })
  @Get('range')
  async getRange(
    @CurrentUser() user: JwtPayload,
    @Query() query: ActivityLogQueryDto,
  ) {
    const fromDate = query.fromDate || new Date().toISOString().split('T')[0];
    const toDate = query.toDate || fromDate;
    return this.activityLogsService.getRange(user.sub, fromDate, toDate);
  }

  @ApiOperation({ summary: 'Log steps count for today' })
  @Patch('steps')
  async logSteps(@CurrentUser() user: JwtPayload, @Body() dto: LogStepsDto) {
    return this.activityLogsService.logSteps(user.sub, dto);
  }

  @ApiOperation({ summary: 'Log calories burned for today' })
  @Patch('calories-burned')
  async logCaloriesBurned(
    @CurrentUser() user: JwtPayload,
    @Body() dto: LogCaloriesBurnedDto,
  ) {
    return this.activityLogsService.logCaloriesBurned(user.sub, dto);
  }

  @ApiOperation({ summary: 'Log water intake for today' })
  @Patch('water')
  async logWater(@CurrentUser() user: JwtPayload, @Body() dto: LogWaterDto) {
    return this.activityLogsService.logWater(user.sub, dto);
  }
}
