import { IsIn, IsInt, IsOptional, IsString, IsUrl, Min } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class CreateBlogBlockDto {
  @ApiProperty({ description: 'Display order (0-indexed)' })
  @IsInt()
  @Min(0)
  order!: number;

  @ApiProperty({ enum: ['text', 'image'] })
  @IsIn(['text', 'image'])
  type!: 'text' | 'image';

  @ApiProperty({ required: false })
  @IsString()
  @IsOptional()
  text_content?: string;

  /** base64 data-URI, e.g. "data:image/jpeg;base64,..." — uploaded to Cloudinary on the server */
  @ApiProperty({ required: false })
  @IsString()
  @IsOptional()
  image_base64?: string;

  /** External URL — admin may pass a direct URL instead of base64 */
  @ApiProperty({ required: false })
  @IsUrl()
  @IsOptional()
  image_url?: string;
}
