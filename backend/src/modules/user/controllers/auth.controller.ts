import { Controller, Post, Body, UseGuards, Get, Req, Res, HttpCode, HttpStatus } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { Throttle } from '@nestjs/throttler';
import { ApiTags, ApiBearerAuth, ApiOperation, ApiExcludeEndpoint } from '@nestjs/swagger';
import { JwtAuthGuard } from '../../../common/guards/jwt.guard';
import { CurrentUser } from '../../../common/decorators/current-user.decorator';
import type { JwtPayload } from '../../../common/interfaces/jwt-payload.interface';
import { AuthService } from '../services/auth.service';
import { RegisterDto } from '../dto/register.dto';
import { LoginDto } from '../dto/login.dto';
import { AuthResponseDto } from '../dto/auth-response.dto';
import { GoogleMobileLoginDto } from '../dto/google-mobile-login.dto';
import {
  ForgotPasswordDto,
  ResetPasswordDto,
  VerifyEmailDto,
  SendVerificationDto,
} from '../dto/email-auth.dto';
import type { GoogleProfile } from '../strategies/google.strategy';

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

  @ApiOperation({ summary: 'Logout — pass refresh_token in body to revoke only current device' })
  @ApiBearerAuth('access-token')
  @UseGuards(JwtAuthGuard)
  @HttpCode(HttpStatus.OK)
  @Post('logout')
  async logout(
    @CurrentUser() user: JwtPayload,
    @Body('refresh_token') refreshToken?: string,
  ): Promise<{ message: string }> {
    await this.authService.logout(user.sub, refreshToken);
    return { message: 'Logged out successfully' };
  }

  @ApiOperation({ summary: 'Send email verification link' })
  @Post('send-verification')
  sendVerification(
    @Body() dto: SendVerificationDto,
  ): Promise<{ message: string }> {
    return this.authService.sendEmailVerification(dto.email);
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

  @ApiOperation({ summary: 'Google Sign-In for mobile — exchange Google ID token for app JWT tokens' })
  @Throttle({ short: { ttl: 60000, limit: 10 } })
  @Post('google-mobile')
  googleMobile(@Body() dto: GoogleMobileLoginDto): Promise<AuthResponseDto> {
    return this.authService.googleMobileLogin(dto.id_token);
  }

  @ApiOperation({ summary: 'Redirect to Google OAuth consent screen' })
  @ApiExcludeEndpoint()
  @UseGuards(AuthGuard('google'))
  @Get('google')
  googleAuth(): void {
    // Passport redirects automatically
  }

  @ApiOperation({ summary: 'Google OAuth callback — redirects to frontend with tokens' })
  @ApiExcludeEndpoint()
  @UseGuards(AuthGuard('google'))
  @Get('google/callback')
  async googleCallback(@Req() req: { user: GoogleProfile }, @Res() res: any): Promise<void> {
    const authData = await this.authService.googleLogin(req.user);
    const frontendUrl = process.env.FRONTEND_URL || 'http://localhost:5173';
    res.redirect(`${frontendUrl}/oauth-callback?accessToken=${authData.access_token}&refreshToken=${authData.refresh_token}`);
  }
}
