import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { IsIn, IsOptional, IsString, MaxLength, MinLength } from 'class-validator';

export class RegisterDeviceDto {
  @ApiProperty({ description: 'FCM registration token' })
  @IsString()
  @MinLength(10)
  @MaxLength(4096)
  token!: string;

  @ApiPropertyOptional({ enum: ['android'], default: 'android' })
  @IsOptional()
  @IsIn(['android'])
  platform?: 'android';
}

export class UnregisterDeviceDto {
  @ApiProperty({ description: 'FCM registration token cần gỡ (khi logout)' })
  @IsString()
  @MinLength(10)
  @MaxLength(4096)
  token!: string;
}
