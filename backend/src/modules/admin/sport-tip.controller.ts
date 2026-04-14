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
import { ApiTags, ApiBearerAuth, ApiOperation, ApiQuery } from '@nestjs/swagger';
import { IsString, IsOptional, IsBoolean, IsArray, IsEnum } from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { AdminService } from './admin.service';
import { JwtAuthGuard } from '../../common/guards/jwt.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { Roles } from '../../common/decorators/roles.decorator';
import { UserRole } from '../../common/enums/user-role.enum';
import { MuscleGroup } from '../../common/enums/muscle-group.enum';

class CreateSportTipDto {
  @ApiProperty() @IsString() title: string;
  @ApiProperty() @IsString() content: string;
  @ApiPropertyOptional() @IsOptional() @IsString() sport_category?: string;
  @ApiPropertyOptional({ enum: MuscleGroup }) @IsOptional() @IsEnum(MuscleGroup) muscle_group?: MuscleGroup;
  @ApiPropertyOptional({ type: [String] }) @IsOptional() @IsArray() tags?: string[];
  @ApiPropertyOptional() @IsOptional() @IsString() thumbnail_url?: string;
  @ApiPropertyOptional() @IsOptional() @IsString() author?: string;
  @ApiPropertyOptional() @IsOptional() @IsBoolean() is_published?: boolean;
}

class UpdateSportTipDto {
  @ApiPropertyOptional() @IsOptional() @IsString() title?: string;
  @ApiPropertyOptional() @IsOptional() @IsString() content?: string;
  @ApiPropertyOptional() @IsOptional() @IsString() sport_category?: string;
  @ApiPropertyOptional({ enum: MuscleGroup }) @IsOptional() @IsEnum(MuscleGroup) muscle_group?: MuscleGroup;
  @ApiPropertyOptional({ type: [String] }) @IsOptional() @IsArray() tags?: string[];
  @ApiPropertyOptional() @IsOptional() @IsString() thumbnail_url?: string;
  @ApiPropertyOptional() @IsOptional() @IsString() author?: string;
  @ApiPropertyOptional() @IsOptional() @IsBoolean() is_published?: boolean;
}

@ApiTags('admin')
@ApiBearerAuth('access-token')
@UseGuards(JwtAuthGuard, RolesGuard)
@Roles(UserRole.ADMIN)
@Controller('admin/sport-tips')
export class SportTipAdminController {
  constructor(private readonly adminService: AdminService) {}

  @ApiOperation({ summary: 'List all sport tips (admin)' })
  @ApiQuery({ name: 'page', required: false })
  @ApiQuery({ name: 'limit', required: false })
  @Get()
  getAll(@Query('page') page?: number, @Query('limit') limit?: number) {
    return this.adminService.getAllSportTips(page ? Number(page) : 1, limit ? Number(limit) : 20);
  }

  @ApiOperation({ summary: 'Create a sport tip' })
  @Post()
  create(@Body() dto: CreateSportTipDto) {
    return this.adminService.createSportTip(dto);
  }

  @ApiOperation({ summary: 'Update a sport tip' })
  @Patch(':id')
  update(@Param('id') id: string, @Body() dto: UpdateSportTipDto) {
    return this.adminService.updateSportTip(id, dto);
  }

  @ApiOperation({ summary: 'Delete a sport tip' })
  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  remove(@Param('id') id: string) {
    return this.adminService.deleteSportTip(id);
  }
}
