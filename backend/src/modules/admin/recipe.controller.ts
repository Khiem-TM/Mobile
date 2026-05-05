import {
  Controller,
  Post,
  Delete,
  Body,
  Param,
  UseGuards,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { AdminService } from './admin.service';
import { JwtAuthGuard } from '../../common/guards/jwt.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { Roles } from '../../common/decorators/roles.decorator';
import { UserRole } from '../../common/enums/user-role.enum';
import { CreateRecipeDto, AddRecipeStepDto } from '../food/dto/create-recipe.dto';

@ApiTags('admin')
@ApiBearerAuth('access-token')
@UseGuards(JwtAuthGuard, RolesGuard)
@Roles(UserRole.ADMIN)
@Controller('admin/foods')
export class RecipeAdminController {
  constructor(private readonly adminService: AdminService) {}

  @ApiOperation({ summary: 'Create or replace recipe for a food (admin)' })
  @Post(':id/recipe')
  upsertRecipe(@Param('id') id: string, @Body() dto: CreateRecipeDto) {
    return this.adminService.upsertFoodRecipe(id, dto);
  }

  @ApiOperation({ summary: 'Add a step to a food recipe (admin)' })
  @Post(':id/recipe/steps')
  addStep(@Param('id') id: string, @Body() dto: AddRecipeStepDto) {
    return this.adminService.addFoodRecipeStep(id, dto);
  }

  @ApiOperation({ summary: 'Delete a recipe step (admin)' })
  @Delete(':id/recipe/steps/:stepId')
  @HttpCode(HttpStatus.NO_CONTENT)
  deleteStep(@Param('stepId') stepId: string) {
    return this.adminService.deleteFoodRecipeStep(stepId);
  }
}
