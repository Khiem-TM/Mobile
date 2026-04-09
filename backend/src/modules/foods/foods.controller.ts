import {
  Controller,
  Get,
  Post,
  Delete,
  Param,
  Query,
  Body,
  UseGuards,
  UseInterceptors,
  UploadedFile,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { ApiTags, ApiBearerAuth, ApiOperation, ApiQuery, ApiConsumes, ApiBody } from '@nestjs/swagger';
import { buildMulterOptions } from '../../common/utils/multer.config';
import { FoodsService } from './foods.service';
import { JwtAuthGuard } from '../../common/guards/jwt.guard';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtPayload } from '../../common/interfaces/jwt-payload.interface';
import { CreateFoodDto } from './dto/create-food.dto';
import { IsString, IsNotEmpty } from 'class-validator';

class RemoveFoodImageDto {
  @IsString() @IsNotEmpty()
  publicId!: string;
}

@ApiTags('foods')
@Controller('foods')
export class FoodsController {
  constructor(private readonly foodsService: FoodsService) {}

  @ApiOperation({ summary: 'Search foods with pagination (public)' })
  @ApiQuery({ name: 'search', required: false })
  @ApiQuery({ name: 'page', required: false })
  @ApiQuery({ name: 'limit', required: false })
  @Get()
  getFoods(
    @Query('search') search: string,
    @Query('page') page = '1',
    @Query('limit') limit = '20',
  ) {
    return this.foodsService.findAll(search, parseInt(page, 10), parseInt(limit, 10));
  }

  @ApiOperation({ summary: 'Get food by barcode (public)' })
  @Get('barcode/:barcode')
  getFoodByBarcode(@Param('barcode') barcode: string) {
    return this.foodsService.findByBarcode(barcode);
  }

  @ApiBearerAuth('access-token')
  @ApiOperation({ summary: 'Get user favorite foods' })
  @UseGuards(JwtAuthGuard)
  @Get('favorites')
  getFavorites(@CurrentUser() user: JwtPayload) {
    return this.foodsService.getFavorites(user.sub);
  }

  @ApiOperation({ summary: 'Get food by ID (public)' })
  @Get(':id')
  getFood(@Param('id') id: string) {
    return this.foodsService.findOne(id);
  }

  @ApiBearerAuth('access-token')
  @ApiOperation({ summary: 'Create custom food' })
  @UseGuards(JwtAuthGuard)
  @Post()
  createCustomFood(@CurrentUser() user: JwtPayload, @Body() body: CreateFoodDto) {
    return this.foodsService.createCustom(user.sub, body);
  }

  @ApiBearerAuth('access-token')
  @ApiOperation({ summary: 'Upload an image for a food item' })
  @ApiConsumes('multipart/form-data')
  @ApiBody({
    schema: {
      type: 'object',
      properties: { file: { type: 'string', format: 'binary' } },
      required: ['file'],
    },
  })
  @UseGuards(JwtAuthGuard)
  @Post(':id/image')
  @UseInterceptors(FileInterceptor('file', buildMulterOptions('foods')))
  uploadFoodImage(
    @Param('id') id: string,
    @UploadedFile() file: Express.Multer.File,
  ) {
    return this.foodsService.uploadImage(id, file);
  }

  @ApiBearerAuth('access-token')
  @ApiOperation({ summary: 'Remove an image from a food item' })
  @UseGuards(JwtAuthGuard)
  @Delete(':id/image')
  removeFoodImage(
    @Param('id') id: string,
    @Body() body: RemoveFoodImageDto,
  ) {
    return this.foodsService.removeImage(id, body.publicId);
  }

  @ApiBearerAuth('access-token')
  @ApiOperation({ summary: 'Add food to favorites' })
  @UseGuards(JwtAuthGuard)
  @Post(':id/favorite')
  async addFavorite(@CurrentUser() user: JwtPayload, @Param('id') id: string) {
    await this.foodsService.addFavorite(user.sub, id);
    return { message: 'Added to favorites' };
  }

  @ApiBearerAuth('access-token')
  @ApiOperation({ summary: 'Remove food from favorites' })
  @UseGuards(JwtAuthGuard)
  @Delete(':id/favorite')
  async removeFavorite(@CurrentUser() user: JwtPayload, @Param('id') id: string) {
    await this.foodsService.removeFavorite(user.sub, id);
    return { message: 'Removed from favorites' };
  }
}
