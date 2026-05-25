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
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import {
  ApiTags,
  ApiBearerAuth,
  ApiOperation,
  ApiQuery,
} from '@nestjs/swagger';
import { TrainingSessionsService } from '../services/training-sessions.service';
import { JwtAuthGuard } from '../../../common/guards/jwt.guard';
import { CurrentUser } from '../../../common/decorators/current-user.decorator';
import type { JwtPayload } from '../../../common/interfaces/jwt-payload.interface';
import { CreateTrainingSessionDto, CreateSessionItemDto } from '../dto/create-training-session.dto';
import { UpdateTrainingSessionDto } from '../dto/update-training-session.dto';
import { UpdateTrainingSessionItemDto } from '../dto/update-training-session-item.dto';

@ApiTags('training-sessions')
@ApiBearerAuth('access-token')
@UseGuards(JwtAuthGuard)
@Controller('training-sessions')
export class TrainingSessionsController {
  constructor(private readonly trainingSessionsService: TrainingSessionsService) {}

  @ApiOperation({ summary: 'Create a training session with one or more exercises' })
  @Post()
  createSession(@CurrentUser() user: JwtPayload, @Body() dto: CreateTrainingSessionDto) {
    return this.trainingSessionsService.createSession(user.sub, dto);
  }

  @ApiOperation({ summary: 'Get training session history (optional date range / limit)' })
  @ApiQuery({ name: 'limit', required: false, example: 20 })
  @ApiQuery({ name: 'fromDate', required: false, example: '2026-01-01' })
  @ApiQuery({ name: 'toDate', required: false, example: '2026-12-31' })
  @Get()
  getSessions(
    @CurrentUser() user: JwtPayload,
    @Query('limit') limit?: string,
    @Query('fromDate') fromDate?: string,
    @Query('toDate') toDate?: string,
  ) {
    if (fromDate || toDate) {
      const today = new Date().toISOString().split('T')[0];
      return this.trainingSessionsService.getSessionsByDateRange(
        user.sub,
        fromDate || today,
        toDate || today,
      );
    }
    return this.trainingSessionsService.getSessions(user.sub, limit ? Number(limit) : 20);
  }

  @ApiOperation({ summary: 'Get training sessions for a specific date' })
  @Get('date/:date')
  getSessionsByDate(@CurrentUser() user: JwtPayload, @Param('date') date: string) {
    return this.trainingSessionsService.getSessionsByDate(user.sub, date);
  }

  @ApiOperation({ summary: 'Get a single training session by ID' })
  @Get(':id')
  getSessionById(@CurrentUser() user: JwtPayload, @Param('id') id: string) {
    return this.trainingSessionsService.getSessionById(user.sub, id);
  }

  @ApiOperation({ summary: 'Update a training session (title / note / date)' })
  @Patch(':id')
  updateSession(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @Body() dto: UpdateTrainingSessionDto,
  ) {
    return this.trainingSessionsService.updateSession(user.sub, id, dto);
  }

  @ApiOperation({ summary: 'Delete a training session' })
  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  deleteSession(@CurrentUser() user: JwtPayload, @Param('id') id: string) {
    return this.trainingSessionsService.deleteSession(user.sub, id);
  }

  @ApiOperation({ summary: 'Add an exercise item to a training session' })
  @Post(':id/items')
  addItem(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @Body() dto: CreateSessionItemDto,
  ) {
    return this.trainingSessionsService.addItem(user.sub, id, dto);
  }

  @ApiOperation({ summary: 'Update an exercise item in a training session' })
  @Patch(':id/items/:itemId')
  updateItem(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @Param('itemId') itemId: string,
    @Body() dto: UpdateTrainingSessionItemDto,
  ) {
    return this.trainingSessionsService.updateItem(user.sub, id, itemId, dto);
  }

  @ApiOperation({ summary: 'Remove an exercise item from a training session' })
  @Delete(':id/items/:itemId')
  @HttpCode(HttpStatus.NO_CONTENT)
  removeItem(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @Param('itemId') itemId: string,
  ) {
    return this.trainingSessionsService.removeItem(user.sub, id, itemId);
  }
}
