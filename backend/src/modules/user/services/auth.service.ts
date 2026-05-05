import {
  Injectable,
  BadRequestException,
  UnauthorizedException,
  NotFoundException,
  ForbiddenException,
} from '@nestjs/common';
import { OAuth2Client } from 'google-auth-library';
import { JwtService } from '@nestjs/jwt';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import * as bcrypt from 'bcrypt';
import { randomBytes } from 'crypto';
import { User } from '../entities/user.entity';
import { RefreshToken } from '../entities/refresh-token.entity';
import { EmailVerification } from '../entities/email-verification.entity';
import { PasswordReset } from '../entities/password-reset.entity';
import { RegisterDto } from '../dto/register.dto';
import { LoginDto } from '../dto/login.dto';
import { AuthResponseDto } from '../dto/auth-response.dto';
import { MailerService } from '../../support/mailer/mailer.service';
import { StreaksService } from './streaks.service';
import { StreakType } from '../../../common/enums/streak-type.enum';
import type { GoogleProfile } from '../strategies/google.strategy';

@Injectable()
export class AuthService {
  constructor(
    @InjectRepository(User)
    private readonly userRepository: Repository<User>,
    @InjectRepository(RefreshToken)
    private readonly refreshTokenRepository: Repository<RefreshToken>,
    @InjectRepository(EmailVerification)
    private readonly emailVerificationRepository: Repository<EmailVerification>,
    @InjectRepository(PasswordReset)
    private readonly passwordResetRepository: Repository<PasswordReset>,
    private readonly jwtService: JwtService,
    private readonly mailerService: MailerService,
    private readonly streaksService: StreaksService,
  ) {}

  //  Tạo token truy cập
  private async generateAuthResponse(user: User): Promise<AuthResponseDto> {
    const jwtSecret = process.env.JWT_SECRET;
    const jwtRefreshSecret = process.env.JWT_REFRESH_SECRET;
    if (!jwtSecret || !jwtRefreshSecret) {
      throw new Error('JWT_SECRET and JWT_REFRESH_SECRET must be defined in environment');
    }

    const ACCESS_TTL_SECONDS = 15 * 60; // 15 minutes

    const access_token = this.jwtService.sign(
      { sub: user.id, email: user.email, role: user.role },
      { secret: jwtSecret, expiresIn: ACCESS_TTL_SECONDS },
    );

    const refresh_token = this.jwtService.sign(
      { sub: user.id },
      { secret: jwtRefreshSecret, expiresIn: '30d' },
    );

    const token_hash = await bcrypt.hash(refresh_token, 10);
    const expiresAt = new Date();
    expiresAt.setDate(expiresAt.getDate() + 30);

    await this.refreshTokenRepository.save({
      user: { id: user.id },
      token_hash,
      expires_at: expiresAt,
    });

    return {
      access_token,
      refresh_token,
      token_type: 'Bearer',
      expires_in: ACCESS_TTL_SECONDS,
      user: {
        id: user.id,
        email: user.email,
        display_name: user.display_name,
        avatar_url: user.avatar_url ?? null,
        role: user.role,
        is_verified: user.is_verified,
      },
    };
  }

  async register(registerDto: RegisterDto): Promise<AuthResponseDto> {
    const { email, password, display_name } = registerDto;

    const existingUser = await this.userRepository.findOne({
      where: { email },
    });
    if (existingUser) {
      throw new BadRequestException('Email already registered');
    }

    const password_hash = await bcrypt.hash(password, 10);
    // Tạo user mới --> Đẩy xuống database
    const user = this.userRepository.create({
      email,
      password_hash,
      display_name,
      is_verified: false,
    });
    await this.userRepository.save(user);

    // Tự động gửi email xác nhận sau khi tạo tài khoản
    // Không dùng await để tránh hold request quá lâu (tùy chọn)
    this.sendEmailVerification(user.email).catch((err) => {
      console.error('Failed to send verification email on register:', err);
    });

    return this.generateAuthResponse(user);
  }

  async login(loginDto: LoginDto): Promise<AuthResponseDto> {
    const { email, password } = loginDto;
    const user = await this.userRepository.findOne({ where: { email } });

    if (!user) {
      throw new UnauthorizedException('Invalid credentials');
    }

    if (!user.password_hash) {
      throw new UnauthorizedException('Tài khoản này đăng nhập bằng Google. Vui lòng sử dụng Google Sign-In.');
    }

    const isPasswordValid = await bcrypt.compare(password, user.password_hash);
    if (!isPasswordValid) {
      throw new UnauthorizedException('Invalid credentials');
    }

    if (!user.is_active) {
      throw new ForbiddenException(
        'Your account has been deactivated. Please contact support.',
      );
    }

    if (!user.is_verified) {
      throw new ForbiddenException(
        'Please verify your email before logging in. Check your inbox or request a new verification email.',
      );
    }

    const today = new Date().toISOString().split('T')[0];
    await this.streaksService.updateActivity(user.id, StreakType.LOGIN, today);

    return this.generateAuthResponse(user);
  }

  async logout(user_sub: string, refreshToken?: string): Promise<void> {
    if (refreshToken) {
      const tokens = await this.refreshTokenRepository.find({
        where: { user: { id: user_sub } },
      });
      for (const record of tokens) {
        if (record.expires_at < new Date()) continue;
        if (await bcrypt.compare(refreshToken, record.token_hash)) {
          await this.refreshTokenRepository.delete(record.id);
          return;
        }
      }
    }
    // Fallback: revoke all tokens (e.g. "logout all devices" or token not provided)
    await this.refreshTokenRepository.delete({ user: { id: user_sub } });
  }

  async refreshToken(token: string): Promise<{ access_token: string; expires_in: number }> {
    const jwtSecret = process.env.JWT_SECRET;
    const jwtRefreshSecret = process.env.JWT_REFRESH_SECRET;
    if (!jwtSecret || !jwtRefreshSecret) {
      throw new Error('JWT_SECRET and JWT_REFRESH_SECRET must be defined in environment');
    }

    try {
      const payload = this.jwtService.verify(token, { secret: jwtRefreshSecret });
      const userId = payload.sub as string;

      const user = await this.userRepository.findOne({ where: { id: userId } });
      if (!user) throw new UnauthorizedException('Invalid refresh token');

      const refreshTokens = await this.refreshTokenRepository.find({
        where: { user: { id: userId } },
      });

      let isValidToken = false;
      for (const record of refreshTokens) {
        if (record.expires_at < new Date()) continue;
        if (await bcrypt.compare(token, record.token_hash)) {
          isValidToken = true;
          break;
        }
      }

      if (!isValidToken) throw new UnauthorizedException('Invalid refresh token');

      const ACCESS_TTL_SECONDS = 15 * 60;
      const access_token = this.jwtService.sign(
        { sub: user.id, email: user.email, role: user.role },
        { secret: jwtSecret, expiresIn: ACCESS_TTL_SECONDS },
      );

      return { access_token, expires_in: ACCESS_TTL_SECONDS };
    } catch (err) {
      if (err instanceof UnauthorizedException) throw err;
      throw new UnauthorizedException('Invalid refresh token');
    }
  }

  async sendEmailVerification(email: string): Promise<{ message: string }> {
    const user = await this.userRepository.findOne({ where: { email } });
    if (!user) {
      // Trả về chung một thông báo để tránh lộ lọt email (Email enumeration attack)
      return {
        message: 'If that email exists, a verification link has been sent.',
      };
    }
    if (user.is_verified) {
      throw new BadRequestException('Email is already verified');
    }

    await this.emailVerificationRepository.delete({ user: { id: user.id } });

    const token = randomBytes(32).toString('hex');
    const expiresAt = new Date(Date.now() + 24 * 60 * 60 * 1000); // 24h

    await this.emailVerificationRepository.save({
      user: { id: user.id },
      token,
      expiresAt,
    });

    await this.mailerService.sendEmailVerification(user.email, token);

    return { message: 'Verification email sent. Check your inbox.' };
  }

  async verifyEmail(token: string): Promise<{ message: string }> {
    const record = await this.emailVerificationRepository.findOne({
      where: { token },
      relations: ['user'], // thêm relations
    });

    if (!record) throw new BadRequestException('Invalid or expired token');
    if (record.usedAt) throw new BadRequestException('Token already used');
    if (record.expiresAt < new Date())
      throw new BadRequestException('Token expired');

    await this.userRepository.update(record.user.id, { is_verified: true });
    await this.emailVerificationRepository.update(record.id, {
      usedAt: new Date(),
    });

    return { message: 'Email verified successfully' };
  }

  // ─── Forgot / Reset Password ─────

  async forgotPassword(email: string): Promise<{ message: string }> {
    const user = await this.userRepository.findOne({ where: { email } });

    if (!user) {
      return { message: 'If that email exists, a reset link has been sent.' };
    }

    await this.passwordResetRepository.delete({ user: { id: user.id } });

    const token = randomBytes(32).toString('hex');
    const expiresAt = new Date(Date.now() + 60 * 60 * 1000); // 1 hour

    await this.passwordResetRepository.save({
      user: { id: user.id },
      token,
      expiresAt,
    });

    await this.mailerService.sendPasswordReset(email, token);

    return { message: 'If that email exists, a reset link has been sent.' };
  }

  async googleMobileLogin(idToken: string): Promise<AuthResponseDto> {
    const clientId = process.env.GOOGLE_CLIENT_ID;
    if (!clientId) throw new Error('GOOGLE_CLIENT_ID not configured');

    const client = new OAuth2Client(clientId);
    let payload: import('google-auth-library').TokenPayload;
    try {
      const ticket = await client.verifyIdToken({ idToken, audience: clientId });
      const p = ticket.getPayload();
      if (!p || !p.email) throw new UnauthorizedException('Invalid Google token');
      payload = p;
    } catch {
      throw new UnauthorizedException('Invalid or expired Google token');
    }

    const googleProfile: GoogleProfile = {
      email: payload.email!,
      display_name: payload.name ?? payload.email!,
      avatar_url: payload.picture ?? null,
      oauth_id: payload.sub,
    };

    return this.googleLogin(googleProfile);
  }

  async googleLogin(googleProfile: GoogleProfile): Promise<AuthResponseDto> {
    const { email, display_name, avatar_url, oauth_id } = googleProfile;

    let user = await this.userRepository.findOne({ where: { email } });

    if (user) {
      if (user.oauth_provider !== 'google') {
        // Link Google to existing email/password account
        await this.userRepository.update(user.id, {
          oauth_provider: 'google',
          oauth_id,
          is_verified: true,
        });
        user.oauth_provider = 'google';
        user.oauth_id = oauth_id;
        user.is_verified = true;
      } else if (user.oauth_id !== oauth_id) {
        await this.userRepository.update(user.id, { oauth_id });
        user.oauth_id = oauth_id;
      }
    } else {
      user = this.userRepository.create({
        email,
        display_name,
        avatar_url: avatar_url ?? undefined,
        oauth_provider: 'google',
        oauth_id,
        is_verified: true,
        is_active: true,
      });
      await this.userRepository.save(user);
    }

    return this.generateAuthResponse(user);
  }

  async resetPassword(
    token: string,
    newPassword: string,
  ): Promise<{ message: string }> {
    const record = await this.passwordResetRepository.findOne({
      where: { token },
      relations: ['user'],
    });

    if (!record) throw new BadRequestException('Invalid or expired token');
    if (record.usedAt) throw new BadRequestException('Token already used');
    if (record.expiresAt < new Date())
      throw new BadRequestException('Token expired');

    const password_hash = await bcrypt.hash(newPassword, 10);
    await this.userRepository.update(record.user.id, { password_hash });
    await this.passwordResetRepository.update(record.id, {
      usedAt: new Date(),
    });

    await this.refreshTokenRepository.delete({ user: { id: record.user.id } });

    return { message: 'Password reset successfully. Please log in again.' };
  }
}
