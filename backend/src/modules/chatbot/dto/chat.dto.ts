import { ApiProperty } from '@nestjs/swagger';
import { IsNotEmpty, IsString } from 'class-validator';

export class SendMessageDto {
  @ApiProperty({ example: 'Hôm nay tôi nên ăn gì để đạt mục tiêu giảm cân?' })
  @IsString()
  @IsNotEmpty()
  message: string;
}
