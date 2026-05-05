import { Type } from 'class-transformer';
import {
  IsArray,
  IsIn,
  IsOptional,
  IsString,
  Matches,
  MaxLength,
  MinLength,
  ValidateNested,
} from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';
import { CreateBlogBlockDto } from './create-blog-block.dto';

export class CreateBlogDto {
  @ApiProperty()
  @IsString()
  @MinLength(1)
  title!: string;

  /** base64 data-URI for the cover thumbnail — uploaded to Cloudinary */
  @ApiProperty({ required: false })
  @IsString()
  @IsOptional()
  thumbnailBase64?: string;

  @ApiProperty({ required: false, type: [String], description: 'Tag strings — no commas allowed' })
  @IsArray()
  @IsString({ each: true })
  @Matches(/^[^,]+$/, { each: true, message: 'Tags cannot contain commas' })
  @MaxLength(50, { each: true })
  @IsOptional()
  tags?: string[];

  /** Users may only set 'draft' or 'pending'. Omit to default to 'pending'. */
  @ApiProperty({ required: false, enum: ['draft', 'pending'] })
  @IsIn(['draft', 'pending'])
  @IsOptional()
  status?: 'draft' | 'pending';

  @ApiProperty({ required: false })
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => CreateBlogBlockDto)
  @IsOptional()
  blocks?: CreateBlogBlockDto[];
}
