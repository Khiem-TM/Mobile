import {
  Controller,
  Get,
  Patch,
  Body,
  Query,
  UseGuards,
} from '@nestjs/common';
import { ApiTags, ApiBearerAuth, ApiOperation, ApiQuery } from '@nestjs/swagger';
import { IsDateString, IsString, IsNumber, Min, Max } from 'class-validator';
import { ActivityLogsService } from '../services/activity-logs.service';
import { JwtAuthGuard } from '../../../common/guards/jwt.guard';
import { CurrentUser } from '../../../common/decorators/current-user.decorator';
import type { JwtPayload } from '../../../common/interfaces/jwt-payload.interface';
import { LogStepsDto } from '../dto/log-steps.dto';
import { LogWaterDto } from '../dto/log-water.dto';
import { ActivityLogQueryDto } from '../dto/activity-log-query.dto';

class LogNoteDto {
  @IsDateString()
  logDate!: string;
  @IsString()
  note!: string;
}

class LogSleepDto {
  @IsDateString()
  logDate!: string;
  @IsNumber()
  @Min(0)
  @Max(24)
  sleepHours!: number;
}

class LogMoodDto {
  @IsDateString()
  logDate!: string;
  @IsString()
  mood!: string;
}

@ApiTags('activity-logs')
@ApiBearerAuth('access-token')
@Controller('activity-logs')
@UseGuards(JwtAuthGuard)
export class ActivityLogsController {
  constructor(private readonly activityLogsService: ActivityLogsService) {}

  @ApiOperation({ summary: 'Get activity log for a specific date' })
  @ApiQuery({ name: 'date', required: false, example: '2026-05-25' })
  @Get()
  getByDate(@CurrentUser() user: JwtPayload, @Query() query: ActivityLogQueryDto) {
    const date = query.date || new Date().toISOString().split('T')[0];
    return this.activityLogsService.getByDate(user.sub, date);
  }

  @ApiOperation({ summary: 'Get activity logs for a date range' })
  @ApiQuery({ name: 'fromDate', required: false })
  @ApiQuery({ name: 'toDate', required: false })
  @Get('range')
  getRange(@CurrentUser() user: JwtPayload, @Query() query: ActivityLogQueryDto) {
    const fromDate = query.fromDate || new Date().toISOString().split('T')[0];
    const toDate = query.toDate || fromDate;
    return this.activityLogsService.getRange(user.sub, fromDate, toDate);
  }

  @ApiOperation({ summary: 'Log steps count for a date' })
  @Patch('steps')
  logSteps(@CurrentUser() user: JwtPayload, @Body() dto: LogStepsDto) {
    return this.activityLogsService.logSteps(user.sub, dto);
  }

  @ApiOperation({ summary: 'Log water intake for a date' })
  @Patch('water')
  logWater(@CurrentUser() user: JwtPayload, @Body() dto: LogWaterDto) {
    return this.activityLogsService.logWater(user.sub, dto);
  }

  @ApiOperation({ summary: 'Log a note for a date' })
  @Patch('note')
  logNote(@CurrentUser() user: JwtPayload, @Body() dto: LogNoteDto) {
    return this.activityLogsService.logNote(user.sub, dto.logDate, dto.note);
  }

  @ApiOperation({ summary: 'Log sleep hours for a date' })
  @Patch('sleep')
  logSleep(@CurrentUser() user: JwtPayload, @Body() dto: LogSleepDto) {
    return this.activityLogsService.logSleep(user.sub, dto.logDate, dto.sleepHours);
  }

  @ApiOperation({ summary: 'Log mood for a date' })
  @Patch('mood')
  logMood(@CurrentUser() user: JwtPayload, @Body() dto: LogMoodDto) {
    return this.activityLogsService.logMood(user.sub, dto.logDate, dto.mood);
  }
}
