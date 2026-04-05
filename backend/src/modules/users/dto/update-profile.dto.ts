import { IsOptional, IsString, Length } from 'class-validator';

export class UpdateProfileDto {
  @IsOptional()
  @IsString()
  @Length(2, 100)
  display_name?: string;

  @IsOptional()
  @IsString()
  avatar_url?: string;
}
