import { IsOptional, IsString } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class RejectBlogDto {
  @ApiProperty({ required: false })
  @IsString()
  @IsOptional()
  reason?: string;
}
