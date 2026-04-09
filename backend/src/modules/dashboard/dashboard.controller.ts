import { Controller, Get, Query, UseGuards } from '@nestjs/common';
import {
  ApiTags,
  ApiBearerAuth,
  ApiOperation,
  ApiQuery,
} from '@nestjs/swagger';
import { DashboardService } from './dashboard.service';
import { JwtAuthGuard } from '../../common/guards/jwt.guard';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtPayload } from '../../common/interfaces/jwt-payload.interface';

@ApiTags('dashboard')
@ApiBearerAuth('access-token')
@UseGuards(JwtAuthGuard)
@Controller('dashboard')
export class DashboardController {
  constructor(private readonly dashboardService: DashboardService) {}

  @ApiOperation({ summary: 'Get daily dashboard summary' })
  @ApiQuery({ name: 'date', required: false, example: '2024-01-15' })
  @Get()
  getDailyDashboard(
    @CurrentUser() user: JwtPayload,
    @Query('date') date?: string,
  ) {
    const targetDate = date ?? new Date().toISOString().split('T')[0];
    return this.dashboardService.getUserDailyDashboard(user.sub, targetDate);
  }

  @ApiOperation({ summary: 'Get weekly report (7 days from weekStart)' })
  @ApiQuery({ name: 'weekStart', required: true, example: '2024-01-15' })
  @Get('weekly')
  getWeeklyReport(
    @CurrentUser() user: JwtPayload,
    @Query('weekStart') weekStart: string,
  ) {
    const start = weekStart ?? new Date().toISOString().split('T')[0];
    return this.dashboardService.getWeeklyReport(user.sub, start);
  }

  @ApiOperation({ summary: 'Get monthly report' })
  @ApiQuery({ name: 'year', required: true, example: 2024 })
  @ApiQuery({ name: 'month', required: true, example: 1 })
  @Get('monthly')
  getMonthlyReport(
    @CurrentUser() user: JwtPayload,
    @Query('year') year: string,
    @Query('month') month: string,
  ) {
    const now = new Date();
    return this.dashboardService.getMonthlyReport(
      user.sub,
      year ? Number(year) : now.getFullYear(),
      month ? Number(month) : now.getMonth() + 1,
    );
  }
}
