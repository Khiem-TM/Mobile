import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import {
  IsArray,
  IsInt,
  IsNotEmpty,
  IsOptional,
  IsString,
  Min,
  ValidateNested,
} from 'class-validator';
import { Type } from 'class-transformer';

export class CreateRecipeStepDto {
  @ApiProperty({ example: 1 })
  @IsInt() @Min(1)
  step_number: number;

  @ApiProperty({ example: 'Rửa sạch rau và để ráo nước.' })
  @IsString() @IsNotEmpty()
  instruction: string;
}

export class CreateRecipeDto {
  @ApiPropertyOptional({ example: 15 })
  @IsOptional() @IsInt() @Min(0)
  prep_time_min?: number;

  @ApiPropertyOptional({ example: 30 })
  @IsOptional() @IsInt() @Min(0)
  cook_time_min?: number;

  @ApiPropertyOptional({ example: 2 })
  @IsOptional() @IsInt() @Min(1)
  servings?: number;

  @ApiPropertyOptional({ type: [CreateRecipeStepDto] })
  @IsOptional()
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => CreateRecipeStepDto)
  steps?: CreateRecipeStepDto[];
}

export class AddRecipeStepDto {
  @ApiProperty({ example: 1 })
  @IsInt() @Min(1)
  step_number: number;

  @ApiProperty({ example: 'Đun sôi nước, cho mì vào nấu 3 phút.' })
  @IsString() @IsNotEmpty()
  instruction: string;

  @ApiPropertyOptional({ description: 'base64 data-URI for step image — uploaded to Cloudinary' })
  @IsOptional()
  @IsString()
  image_base64?: string;
}
