import { IsEmail, IsString } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class AdminLoginDto {
  @ApiProperty({ example: 'admin@gmail.com' })
  @IsEmail()
  email!: string;

  @ApiProperty({ example: 'admin' })
  @IsString()
  password!: string;
}
