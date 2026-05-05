import { IsDateString, IsInt, Min, Max } from 'class-validator';

// buoc chan theo ngay
export class LogStepsDto {
  @IsDateString()
  logDate!: string;

  @IsInt()
  @Min(0)
  @Max(100000)
  steps!: number;
}
