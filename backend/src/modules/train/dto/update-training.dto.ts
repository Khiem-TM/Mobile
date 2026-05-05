import { IsOptional, IsString } from 'class-validator';
import { ApiPropertyOptional } from '@nestjs/swagger';

export class UpdateWorkoutSessionDto {
  @ApiPropertyOptional({ example: 'Evening Pull Day' })
  @IsOptional()
  @IsString()
  sessionName?: string;

  @ApiPropertyOptional({ example: 'Felt a bit tired today' })
  @IsOptional()
  @IsString()
  notes?: string;
}
