import { IsString, IsNotEmpty } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class GoogleMobileLoginDto {
  @ApiProperty({ description: 'Google ID token obtained from mobile Google Sign-In SDK' })
  @IsString()
  @IsNotEmpty()
  id_token: string;
}
