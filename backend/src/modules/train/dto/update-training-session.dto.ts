import { IsString, IsOptional, IsDateString } from 'class-validator';
import { ApiPropertyOptional } from '@nestjs/swagger';

export class UpdateTrainingSessionDto {
  @ApiPropertyOptional({ example: '2026-05-25' })
  @IsOptional()
  @IsDateString()
  sessionDate?: string;

  @ApiPropertyOptional({ example: 'Push Day' })
  @IsOptional()
  @IsString()
  title?: string;

  @ApiPropertyOptional({ example: 'Great energy today' })
  @IsOptional()
  @IsString()
  note?: string;
}
