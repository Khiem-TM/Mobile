import { Controller, Post, Body, UseGuards } from '@nestjs/common';
import { Throttle } from '@nestjs/throttler';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { JwtAuthGuard } from '../../common/guards/jwt.guard';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtPayload } from '../../common/interfaces/jwt-payload.interface';
import { AuthService } from './auth.service';
import { RegisterDto } from './dto/register.dto';
import { LoginDto } from './dto/login.dto';
import { AuthResponseDto } from './dto/auth-response.dto';
import {
  ForgotPasswordDto,
  ResetPasswordDto,
  VerifyEmailDto,
} from './dto/email-auth.dto';

@ApiTags('auth')
@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @ApiOperation({ summary: 'Register a new account' })
  @Throttle({ short: { ttl: 60000, limit: 5 } })
  @Post('register')
  register(@Body() registerDto: RegisterDto): Promise<AuthResponseDto> {
    return this.authService.register(registerDto);
  }

  @ApiOperation({ summary: 'Login' })
  @Throttle({ short: { ttl: 60000, limit: 10 } })
  @Post('login')
  login(@Body() loginDto: LoginDto): Promise<AuthResponseDto> {
    return this.authService.login(loginDto);
  }

  @ApiOperation({ summary: 'Refresh access token' })
  @Post('refresh')
  refresh(
    @Body('refresh_token') token: string,
  ): Promise<{ access_token: string }> {
    return this.authService.refreshToken(token);
  }

  @ApiOperation({ summary: 'Logout' })
  @ApiBearerAuth('access-token')
  @UseGuards(JwtAuthGuard)
  @Post('logout')
  async logout(@CurrentUser() user: JwtPayload): Promise<{ message: string }> {
    await this.authService.logout(user.sub);
    return { message: 'Logged out successfully' };
  }

  @ApiOperation({ summary: 'Send email verification link' })
  @ApiBearerAuth('access-token')
  @UseGuards(JwtAuthGuard)
  @Post('send-verification')
  sendVerification(
    @CurrentUser() user: JwtPayload,
  ): Promise<{ message: string }> {
    return this.authService.sendEmailVerification(user.sub);
  }

  @ApiOperation({ summary: 'Verify email with token' })
  @Post('verify-email')
  verifyEmail(@Body() dto: VerifyEmailDto): Promise<{ message: string }> {
    return this.authService.verifyEmail(dto.token);
  }

  @ApiOperation({ summary: 'Request password reset email' })
  @Throttle({ short: { ttl: 60000, limit: 3 } })
  @Post('forgot-password')
  forgotPassword(@Body() dto: ForgotPasswordDto): Promise<{ message: string }> {
    return this.authService.forgotPassword(dto.email);
  }

  @ApiOperation({ summary: 'Reset password using token' })
  @Post('reset-password')
  resetPassword(@Body() dto: ResetPasswordDto): Promise<{ message: string }> {
    return this.authService.resetPassword(dto.token, dto.newPassword);
  }
}
