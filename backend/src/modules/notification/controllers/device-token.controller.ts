import {
  Body,
  Controller,
  Delete,
  HttpCode,
  HttpStatus,
  Post,
  UseGuards,
} from '@nestjs/common';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { DeviceTokenService } from '../services/device-token.service';
import { RegisterDeviceDto, UnregisterDeviceDto } from '../dto/register-device.dto';
import { JwtAuthGuard } from '../../../common/guards/jwt.guard';
import { CurrentUser } from '../../../common/decorators/current-user.decorator';
import type { JwtPayload } from '../../../common/interfaces/jwt-payload.interface';

@ApiTags('devices')
@ApiBearerAuth('access-token')
@UseGuards(JwtAuthGuard)
@Controller('devices')
export class DeviceTokenController {
  constructor(private readonly deviceTokenService: DeviceTokenService) {}

  @ApiOperation({ summary: 'Đăng ký/cập nhật FCM token của thiết bị' })
  @Post('token')
  @HttpCode(HttpStatus.NO_CONTENT)
  async register(
    @CurrentUser() user: JwtPayload,
    @Body() dto: RegisterDeviceDto,
  ): Promise<void> {
    await this.deviceTokenService.upsert(
      user.sub,
      dto.token,
      dto.platform ?? 'android',
    );
  }

  @ApiOperation({ summary: 'Gỡ FCM token (khi logout)' })
  @Delete('token')
  @HttpCode(HttpStatus.NO_CONTENT)
  async unregister(@Body() dto: UnregisterDeviceDto): Promise<void> {
    await this.deviceTokenService.remove(dto.token);
  }
}
