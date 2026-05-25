import {
  Controller,
  Get,
  Post,
  Delete,
  Param,
  UseGuards,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { FavoriteExercisesService } from '../services/favorite-exercises.service';
import { JwtAuthGuard } from '../../../common/guards/jwt.guard';
import { CurrentUser } from '../../../common/decorators/current-user.decorator';
import type { JwtPayload } from '../../../common/interfaces/jwt-payload.interface';

@ApiTags('favorite-exercises')
@ApiBearerAuth('access-token')
@UseGuards(JwtAuthGuard)
@Controller('favorite-exercises')
export class FavoriteExercisesController {
  constructor(private readonly favoriteExercisesService: FavoriteExercisesService) {}

  @ApiOperation({ summary: 'Get all favorite exercises for the current user' })
  @Get()
  getFavorites(@CurrentUser() user: JwtPayload) {
    return this.favoriteExercisesService.getFavorites(user.sub);
  }

  @ApiOperation({ summary: 'Check if an exercise is in favorites' })
  @Get('check/:exerciseId')
  checkIsFavorite(@CurrentUser() user: JwtPayload, @Param('exerciseId') exerciseId: string) {
    return this.favoriteExercisesService.checkIsFavorite(user.sub, exerciseId);
  }

  @ApiOperation({ summary: 'Add an exercise to favorites' })
  @Post(':exerciseId')
  @HttpCode(HttpStatus.CREATED)
  async addFavorite(@CurrentUser() user: JwtPayload, @Param('exerciseId') exerciseId: string) {
    await this.favoriteExercisesService.addFavorite(user.sub, exerciseId);
    return { message: 'Added to favorites' };
  }

  @ApiOperation({ summary: 'Remove an exercise from favorites' })
  @Delete(':exerciseId')
  @HttpCode(HttpStatus.NO_CONTENT)
  removeFavorite(@CurrentUser() user: JwtPayload, @Param('exerciseId') exerciseId: string) {
    return this.favoriteExercisesService.removeFavorite(user.sub, exerciseId);
  }
}
