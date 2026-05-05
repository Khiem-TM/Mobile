import {
  IsDateString,
  IsNumber,
  IsInt,
  IsOptional,
  IsString,
  Min,
  Max,
  MaxLength,
} from 'class-validator';

// log calories burned theo ngày
export class LogCaloriesBurnedDto {
  @IsDateString()
  logDate!: string;

  @IsNumber()
  @Min(0)
  @Max(10000)
  caloriesBurned!: number;

  @IsInt()
  @Min(0)
  @Max(1440)
  activeMinutes!: number;

  @IsOptional()
  @IsString()
  @MaxLength(500)
  exerciseNotes?: string;
}
