import {
  Controller,
  Get,
  Put,
  Patch,
  Post,
  Body,
  Param,
  UseGuards,
  ForbiddenException,
  NotFoundException,
  UseInterceptors,
  UploadedFile,
  BadRequestException,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import {
  ApiTags,
  ApiBearerAuth,
  ApiOperation,
  ApiConsumes,
  ApiBody,
} from '@nestjs/swagger';
import { UsersService } from '../services/users.service';
import { JwtAuthGuard } from '../../../common/guards/jwt.guard';
import { CurrentUser } from '../../../common/decorators/current-user.decorator';
import type { JwtPayload } from '../../../common/interfaces/jwt-payload.interface';
import { UpdateProfileDto } from '../dto/update-profile.dto';
import { UpdateHealthProfileDto } from '../dto/update-health-profile.dto';
import { User } from '../entities/user.entity';
import { UserHealthProfile } from '../entities/user-health-profile.entity';
import { buildMulterOptions } from '../../../common/utils/multer.config';
import { CloudinaryService } from '../../support/cloudinary/cloudinary.service';

@ApiTags('users')
@ApiBearerAuth('access-token')
@UseGuards(JwtAuthGuard)
@Controller('users')
export class UsersController {
  constructor(
    private readonly usersService: UsersService,
    private readonly cloudinaryService: CloudinaryService,
  ) {}

  @ApiOperation({ summary: 'Get current user profile' })
  @Get('me')
  getCurrentUser(@CurrentUser() user: JwtPayload): Promise<User> {
    return this.usersService.findById(user.sub);
  }

  @ApiOperation({ summary: 'Update user profile' })
  @Patch(':id')
  async updateProfile(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @Body() dto: UpdateProfileDto,
  ): Promise<User> {
    if (user.sub !== id) {
      throw new ForbiddenException('You can only update your own profile');
    }
    return this.usersService.updateProfile(user.sub, dto);
  }

  @ApiOperation({ summary: 'Upload avatar image' })
  @ApiConsumes('multipart/form-data')
  @ApiBody({
    schema: {
      type: 'object',
      properties: { file: { type: 'string', format: 'binary' } },
    },
  })
  @Post('me/avatar')
  @UseInterceptors(FileInterceptor('file', buildMulterOptions('avatars')))
  async uploadAvatar(
    @CurrentUser() user: JwtPayload,
    @UploadedFile() file: Express.Multer.File & { filename: string },
  ): Promise<User> {
    if (!file) {
      throw new BadRequestException('No file uploaded');
    }
    const { url } = await this.cloudinaryService.uploadFile(file, 'avatars');
    return this.usersService.updateProfile(user.sub, { avatar_url: url });
  }

  // Vo hieu hoa account
  @ApiOperation({ summary: 'Deactivate account' })
  @Patch(':id/deactivate')
  async deactivateAccount(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
  ): Promise<{ message: string }> {
    if (user.sub !== id) {
      throw new ForbiddenException('You can only deactivate your own account');
    }
    await this.usersService.deactivateAccount(user.sub);
    return { message: 'Account deactivated successfully' };
  }

  @ApiOperation({ summary: 'Get health profile of current user' })
  @Get('me/health-profile')
  async getHealthProfile(
    @CurrentUser() user: JwtPayload,
  ): Promise<UserHealthProfile> {
    const profile = await this.usersService.getHealthProfile(user.sub);
    if (!profile) {
      throw new NotFoundException(
        'Health profile not found. Please create one first.',
      );
    }
    return profile;
  }

  @ApiOperation({ summary: 'Create or update health profile' })
  @Put('me/health-profile')
  updateHealthProfile(
    @CurrentUser() user: JwtPayload,
    @Body() dto: UpdateHealthProfileDto,
  ): Promise<UserHealthProfile> {
    return this.usersService.updateHealthProfile(user.sub, dto);
  }
}
