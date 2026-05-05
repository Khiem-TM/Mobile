import { IsDateString, IsInt, Min, Max } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

// log lượng nước uống theo ngày
export class LogWaterDto {
  @IsDateString()
  logDate!: string;

  @ApiProperty({ description: 'Water intake in milliliters', example: 250 })
  @IsInt()
  @Min(0)
  @Max(10000)
  waterMl!: number;
}
