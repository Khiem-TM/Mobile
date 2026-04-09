import { IsString, IsOptional, IsUrl, MinLength } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class UpdateBlogDto {
  @ApiProperty({ required: false })
  @IsString()
  @MinLength(1)
  @IsOptional()
  title?: string;

  @ApiProperty({ required: false })
  @IsString()
  @IsOptional()
  content?: string;

  @ApiProperty({ required: false })
  @IsString()
  @IsOptional()
  author?: string;

  @ApiProperty({ required: false })
  @IsUrl()
  @IsOptional()
  thumbnailUrl?: string;
}
