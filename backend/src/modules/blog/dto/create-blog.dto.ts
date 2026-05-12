import { Type } from 'class-transformer';
import {
  IsArray,
  IsIn,
  IsOptional,
  IsString,
  IsUrl,
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

  /** Direct URL for the thumbnail (admin use) */
  @ApiProperty({ required: false })
  @IsUrl()
  @IsOptional()
  thumbnailUrl?: string;

  @ApiProperty({ required: false, type: [String], description: 'Tag strings — no commas allowed' })
  @IsArray()
  @IsString({ each: true })
  @Matches(/^[^,]+$/, { each: true, message: 'Tags cannot contain commas' })
  @MaxLength(50, { each: true })
  @IsOptional()
  tags?: string[];

  /** Users may set 'draft' to save without publishing. Omit to publish immediately (approved). */
  @ApiProperty({ required: false, enum: ['draft'] })
  @IsIn(['draft'])
  @IsOptional()
  status?: 'draft';

  @ApiProperty({ required: false })
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => CreateBlogBlockDto)
  @IsOptional()
  blocks?: CreateBlogBlockDto[];
}
