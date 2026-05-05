import {
  Controller,
  Get,
  Patch,
  Delete,
  Param,
  Query,
  UseGuards,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import { ApiTags, ApiBearerAuth, ApiOperation, ApiQuery } from '@nestjs/swagger';
import { NotificationsService } from '../services/notifications.service';
import { JwtAuthGuard } from '../../../common/guards/jwt.guard';
import { CurrentUser } from '../../../common/decorators/current-user.decorator';
import type { JwtPayload } from '../../../common/interfaces/jwt-payload.interface';

@ApiTags('notifications')
@ApiBearerAuth('access-token')
@UseGuards(JwtAuthGuard)
@Controller('notifications')
export class NotificationsController {
  constructor(private readonly notificationsService: NotificationsService) {}

  @ApiOperation({ summary: 'Get notifications for current user' })
  @ApiQuery({ name: 'unread', required: false, type: Boolean })
  @Get()
  getNotifications(
    @CurrentUser() user: JwtPayload,
    @Query('unread') unread?: string,
  ) {
    return this.notificationsService.getForUser(user.sub, unread === 'true');
  }

  @ApiOperation({ summary: 'Get unread notification count' })
  @Get('unread-count')
  getUnreadCount(@CurrentUser() user: JwtPayload) {
    return this.notificationsService.getUnreadCount(user.sub);
  }

  @ApiOperation({ summary: 'Mark all notifications as read' })
  @Patch('read-all')
  markAllAsRead(@CurrentUser() user: JwtPayload) {
    return this.notificationsService.markAllAsRead(user.sub);
  }

  @ApiOperation({ summary: 'Mark a notification as read' })
  @Patch(':id/read')
  markAsRead(@CurrentUser() user: JwtPayload, @Param('id') id: string) {
    return this.notificationsService.markAsRead(user.sub, id);
  }

  @ApiOperation({ summary: 'Delete a notification' })
  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  deleteNotification(@CurrentUser() user: JwtPayload, @Param('id') id: string) {
    return this.notificationsService.deleteNotification(user.sub, id);
  }
}
