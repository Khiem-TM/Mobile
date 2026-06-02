import { IsOptional, IsString, MaxLength, MinLength } from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

export class AdminWarningDto {
  @ApiProperty({ example: 'Cảnh báo vi phạm nội dung' })
  @IsString()
  @MinLength(1)
  @MaxLength(200)
  title!: string;

  @ApiProperty({ example: 'Vui lòng chỉnh sửa nội dung theo quy định cộng đồng.' })
  @IsString()
  @MinLength(1)
  body!: string;

  @ApiPropertyOptional({ example: 'blog:spam' })
  @IsString()
  @MaxLength(100)
  @IsOptional()
  reasonCode?: string;
}
