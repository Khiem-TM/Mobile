import { IsString, IsNotEmpty } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class UploadImageDto {
  @ApiProperty({
    description: 'Base64 encoded image or full data-URI (data:image/...;base64,...)',
    example: 'data:image/jpeg;base64,/9j/4AAQSkZJRgAB...',
  })
  @IsString()
  @IsNotEmpty()
  imageData!: string;
}
