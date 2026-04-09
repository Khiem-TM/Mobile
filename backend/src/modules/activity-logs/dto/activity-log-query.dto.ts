import { IsOptional, IsDateString } from 'class-validator';

// log activity theo ngày hoặc khoảng thời gian
export class ActivityLogQueryDto {
  @IsOptional()
  @IsDateString()
  date?: string;

  @IsOptional()
  @IsDateString()
  fromDate?: string;

  @IsOptional()
  @IsDateString()
  toDate?: string;
}
