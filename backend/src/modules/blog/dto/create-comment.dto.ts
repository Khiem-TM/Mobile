import { IsString, MinLength, MaxLength } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class CreateCommentDto {
  @ApiProperty({ description: 'Comment text content' })
  @IsString()
  @MinLength(1)
  @MaxLength(2000)
  content!: string;
}
