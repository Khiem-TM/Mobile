import { IsDateString, IsOptional, IsString } from 'class-validator';
import { ApiPropertyOptional } from '@nestjs/swagger';

export class UpdateWorkoutSessionDto {
  @ApiPropertyOptional({ example: '2026-04-27' })
  @IsOptional()
  @IsDateString()
  sessionDate?: string;

  @ApiPropertyOptional({ example: 'Evening Pull Day' })
  @IsOptional()
  @IsString()
  sessionName?: string;

  @ApiPropertyOptional({ example: 'Felt a bit tired today' })
  @IsOptional()
  @IsString()
  notes?: string;
}
