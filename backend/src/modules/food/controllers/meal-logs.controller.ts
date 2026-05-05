import {
  Controller,
  Get,
  Post,
  Body,
  Param,
  Delete,
  Query,
  UseGuards,
  UseInterceptors,
  UploadedFile,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import { Patch } from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { buildMulterOptions } from '../../../common/utils/multer.config';
import { ApiTags, ApiBearerAuth, ApiOperation, ApiQuery, ApiConsumes, ApiBody } from '@nestjs/swagger';
import { MealLogsService } from '../services/meal-logs.service';
import {
  CreateMealLogDto,
  CreateMealLogItemDto,
} from '../dto/create-meal-log.dto';
import { UpdateMealLogDto, UpdateMealLogItemDto } from '../dto/update-meal-log.dto';
import { UploadImageDto } from '../dto/upload-image.dto';
import { JwtAuthGuard } from '../../../common/guards/jwt.guard';
import { CurrentUser } from '../../../common/decorators/current-user.decorator';
import type { JwtPayload } from '../../../common/interfaces/jwt-payload.interface';

@ApiTags('meal-logs')
@ApiBearerAuth('access-token')
@UseGuards(JwtAuthGuard)
@Controller('meal-logs')
export class MealLogsController {
  constructor(private readonly mealLogsService: MealLogsService) {}

  @ApiOperation({ summary: 'Create a meal log' })
  @Post()
  create(
    @CurrentUser() user: JwtPayload,
    @Body() dto: CreateMealLogDto,
  ) {
    return this.mealLogsService.create(user.sub, dto);
  }

  @ApiOperation({ summary: 'Get all meal logs (optionally filter by date)' })
  @ApiQuery({ name: 'date', required: false, example: '2024-01-15' })
  @Get()
  findAll(@CurrentUser() user: JwtPayload, @Query('date') date?: string) {
    return this.mealLogsService.findAllByUser(user.sub, date);
  }

  @ApiOperation({ summary: 'Get meal log history for a date range' })
  @ApiQuery({ name: 'fromDate', required: false, example: '2024-01-01' })
  @ApiQuery({ name: 'toDate', required: false, example: '2024-01-31' })
  @Get('history')
  getHistory(
    @CurrentUser() user: JwtPayload,
    @Query('fromDate') fromDate?: string,
    @Query('toDate') toDate?: string,
  ) {
    return this.mealLogsService.findHistory(user.sub, fromDate, toDate);
  }

  @ApiOperation({ summary: 'Get meal logs for a specific date' })
  @Get('history/:date')
  getHistoryByDate(
    @CurrentUser() user: JwtPayload,
    @Param('date') date: string,
  ) {
    return this.mealLogsService.findAllByUser(user.sub, date);
  }

  @ApiOperation({ summary: 'Get daily nutrition summary (defaults to today)' })
  @ApiQuery({ name: 'date', required: false, example: '2024-01-15' })
  @Get('summary')
  getSummary(@CurrentUser() user: JwtPayload, @Query('date') date?: string) {
    return this.mealLogsService.getDailySummary(user.sub, date);
  }

  @ApiOperation({ summary: 'Get a specific meal log' })
  @Get(':id')
  findOne(@CurrentUser() user: JwtPayload, @Param('id') id: string) {
    return this.mealLogsService.findOne(user.sub, id);
  }

  @ApiOperation({ summary: 'Update a meal log (notes, meal_type)' })
  @Patch(':id')
  updateLog(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @Body() dto: UpdateMealLogDto,
  ) {
    return this.mealLogsService.updateLog(user.sub, id, dto);
  }

  @ApiOperation({ summary: 'Delete a meal log and all its items' })
  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  deleteLog(@CurrentUser() user: JwtPayload, @Param('id') id: string) {
    return this.mealLogsService.deleteLog(user.sub, id);
  }

  @ApiOperation({ summary: 'Upload an image for a meal log (base64 / data-URI)' })
  @Post(':id/image/base64')
  uploadBase64Image(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @Body() dto: UploadImageDto,
  ) {
    return this.mealLogsService.uploadBase64Image(user.sub, id, dto);
  }

  @ApiOperation({ summary: 'Upload an image for a meal log (multipart/form-data)' })
  @ApiConsumes('multipart/form-data')
  @ApiBody({
    schema: {
      type: 'object',
      properties: { file: { type: 'string', format: 'binary' } },
      required: ['file'],
    },
  })
  @Post(':id/image')
  @UseInterceptors(FileInterceptor('file', buildMulterOptions('meal-logs')))
  uploadImage(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @UploadedFile() file: Express.Multer.File,
  ) {
    return this.mealLogsService.uploadImage(user.sub, id, file);
  }

  @ApiOperation({ summary: 'Add a food item to a meal log' })
  @Post(':id/items')
  addItem(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @Body() itemDto: CreateMealLogItemDto,
  ) {
    return this.mealLogsService.addItemToLog(user.sub, id, itemDto);
  }

  @ApiOperation({ summary: 'Update quantity/unit of a meal log item' })
  @Patch(':id/items/:itemId')
  updateItem(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @Param('itemId') itemId: string,
    @Body() dto: UpdateMealLogItemDto,
  ) {
    return this.mealLogsService.updateItem(user.sub, id, itemId, dto);
  }

  @ApiOperation({ summary: 'Remove a food item from a meal log' })
  @Delete(':id/items/:itemId')
  @HttpCode(HttpStatus.NO_CONTENT)
  removeItem(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @Param('itemId') itemId: string,
  ) {
    return this.mealLogsService.removeItemFromLog(user.sub, id, itemId);
  }
}
