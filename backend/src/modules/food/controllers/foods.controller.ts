import {
  Controller,
  Get,
  Post,
  Patch,
  Delete,
  Param,
  Query,
  Body,
  UseGuards,
  UseInterceptors,
  UploadedFile,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { ApiTags, ApiBearerAuth, ApiOperation, ApiQuery, ApiConsumes, ApiBody } from '@nestjs/swagger';
import { buildMulterOptions } from '../../../common/utils/multer.config';
import { FoodsService } from '../services/foods.service';
import { JwtAuthGuard } from '../../../common/guards/jwt.guard';
import { CurrentUser } from '../../../common/decorators/current-user.decorator';
import type { JwtPayload } from '../../../common/interfaces/jwt-payload.interface';
import { CreateFoodDto, UpdateFoodDto } from '../dto/create-food.dto';
import { IsString, IsNotEmpty } from 'class-validator';
import { Public } from '../../../common/decorators/public.decorator';

class RemoveFoodImageDto {
  @IsString() @IsNotEmpty()
  publicId!: string;
}

@ApiTags('foods')
@Controller('foods')
export class FoodsController {
  constructor(private readonly foodsService: FoodsService) {}

  @ApiOperation({ summary: 'Explore dishes with images and descriptions (public)' })
  @ApiQuery({ name: 'page', required: false })
  @ApiQuery({ name: 'limit', required: false })
  @ApiQuery({ name: 'category', required: false })
  @Public()
  @Get('explore')
  exploreDishe(
    @Query('page') page = '1',
    @Query('limit') limit = '20',
    @Query('category') category?: string,
  ) {
    return this.foodsService.exploreDishe(parseInt(page, 10), parseInt(limit, 10), category);
  }

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

  // ─── Custom foods ────────────────────────────────────────────────────────────

  @ApiBearerAuth('access-token')
  @ApiOperation({ summary: 'List my custom foods' })
  @ApiQuery({ name: 'page', required: false })
  @ApiQuery({ name: 'limit', required: false })
  @UseGuards(JwtAuthGuard)
  @Get('custom')
  getMyCustomFoods(
    @CurrentUser() user: JwtPayload,
    @Query('search') search = '',
    @Query('page') page = '1',
    @Query('limit') limit = '20',
  ) {
    return this.foodsService.getUserCustomFoods(
      user.sub,
      search,
      parseInt(page, 10),
      parseInt(limit, 10),
    );
  }

  @ApiBearerAuth('access-token')
  @ApiOperation({ summary: 'Get my custom food by ID' })
  @UseGuards(JwtAuthGuard)
  @Get('custom/:id')
  getMyCustomFood(@CurrentUser() user: JwtPayload, @Param('id') id: string) {
    return this.foodsService.findCustomForUser(id, user.sub);
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
  @ApiOperation({ summary: 'Update my custom food' })
  @UseGuards(JwtAuthGuard)
  @Patch('custom/:id')
  updateCustomFood(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @Body() body: UpdateFoodDto,
  ) {
    return this.foodsService.updateCustom(user.sub, id, body);
  }

  @ApiBearerAuth('access-token')
  @ApiOperation({ summary: 'Delete my custom food' })
  @UseGuards(JwtAuthGuard)
  @Delete('custom/:id')
  @HttpCode(HttpStatus.NO_CONTENT)
  deleteCustomFood(@CurrentUser() user: JwtPayload, @Param('id') id: string) {
    return this.foodsService.deleteCustom(user.sub, id);
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
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @UploadedFile() file: Express.Multer.File,
  ) {
    return this.foodsService.uploadImage(id, user.sub, file);
  }

  @ApiBearerAuth('access-token')
  @ApiOperation({ summary: 'Remove an image from a food item' })
  @UseGuards(JwtAuthGuard)
  @Delete(':id/image')
  removeFoodImage(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @Body() body: RemoveFoodImageDto,
  ) {
    return this.foodsService.removeImage(id, user.sub, body.publicId);
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
