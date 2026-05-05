import { Controller, Get, UseGuards } from '@nestjs/common';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { StreaksService } from '../services/streaks.service';
import { JwtAuthGuard } from '../../../common/guards/jwt.guard';
import { CurrentUser } from '../../../common/decorators/current-user.decorator';
import type { JwtPayload } from '../../../common/interfaces/jwt-payload.interface';

@ApiTags('streaks')
@ApiBearerAuth('access-token')
@Controller('streaks')
@UseGuards(JwtAuthGuard)
export class StreaksController {
  constructor(private readonly streaksService: StreaksService) {}

  @ApiOperation({ summary: 'Get current user streak data' })
  @Get()
  async getMyStreaks(@CurrentUser() user: JwtPayload) {
    return this.streaksService.getStreaks(user.sub);
  }
}
