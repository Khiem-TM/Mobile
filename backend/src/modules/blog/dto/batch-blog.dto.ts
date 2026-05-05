import { IsArray, IsUUID, ArrayMinSize, IsOptional, IsString } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class BatchBlogActionDto {
  @ApiProperty({ type: [String], description: 'Array of blog UUIDs' })
  @IsArray()
  @ArrayMinSize(1)
  @IsUUID('4', { each: true })
  ids!: string[];
}

export class BatchRejectBlogDto {
  @ApiProperty({ type: [String], description: 'Array of blog UUIDs' })
  @IsArray()
  @ArrayMinSize(1)
  @IsUUID('4', { each: true })
  ids!: string[];

  @ApiProperty({ required: false, description: 'Optional rejection reason applied to all' })
  @IsString()
  @IsOptional()
  reason?: string;
}
