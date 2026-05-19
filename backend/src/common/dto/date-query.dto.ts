import {
  IsDateString,
  IsOptional,
  Validate,
  ValidatorConstraint,
  ValidatorConstraintInterface,
  ValidationArguments,
} from 'class-validator';

interface DateValidationObject {
  startDate?: string;
  endDate?: string;
}

@ValidatorConstraint({ name: 'isEndDateAfterStartDate', async: false })
class IsEndDateAfterStartDate implements ValidatorConstraintInterface {
  validate(endDate: string, { object }: ValidationArguments) {
    const { startDate } = object as DateValidationObject;

    // Trong case chỉ cung cấp 1 đầu chặn time
    if (!startDate || !endDate) return true;

    // convert string to date
    const start = new Date(startDate);
    const end = new Date(endDate);

    //chống invalid date
    if (isNaN(start.getTime()) || isNaN(end.getTime())) {
      return false;
    }

    return end >= start;
  }

  defaultMessage(): string {
    return 'endDate cần sau hoặc bằng startDate';
  }
}

export class DateQueryDto {
  @IsOptional()
  @IsDateString()
  startDate?: string;

  @IsOptional()
  @IsDateString()
  @Validate(IsEndDateAfterStartDate)
  endDate?: string;
}
// request theo date --> Làm luôn nhiệm vụ validate date để tránh lỗi khi query database
/*
- Lọc calories in/out theo ngày
- Lọc meal log theo ngày
- Lọc training theo ngày
- Lọc dashboard theo ngày
*/
