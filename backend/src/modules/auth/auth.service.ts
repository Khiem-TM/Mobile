import {
  Injectable,
  BadRequestException,
  UnauthorizedException,
  NotFoundException,
  Logger,
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { InjectRepository } from '@nestjs/typeorm';
import { In, Repository } from 'typeorm';
import * as bcrypt from 'bcrypt';
import { randomBytes } from 'crypto';
import { User } from '../users/entities/user.entity';
import { RefreshToken } from './entities/refresh-token.entity';
import { EmailVerification } from './entities/email-verification.entity';
import { PasswordReset } from './entities/password-reset.entity';
import { RegisterDto } from './dto/register.dto';
import { LoginDto } from './dto/login.dto';
import { AuthResponseDto } from './dto/auth-response.dto';

@Injectable()
export class AuthService {
  private readonly logger = new Logger(AuthService.name);

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
  ) {}

  //  Tạo token truy cập
  private async generateAuthResponse(user: User): Promise<AuthResponseDto> {
    const access_token = this.jwtService.sign(
      { sub: user.id, email: user.email, role: user.role },
      { secret: process.env.JWT_SECRET || 'khiemhehe', expiresIn: '15m' },
    );

    const refresh_token = this.jwtService.sign(
      { sub: user.id },
      {
        secret: process.env.JWT_REFRESH_SECRET || 'khiemhehe',
        expiresIn: '30d',
      },
    );

    const token_hash = await bcrypt.hash(refresh_token, 10);
    const expiresAt = new Date();
    expiresAt.setDate(expiresAt.getDate() + 30);

    await this.refreshTokenRepository.save({
      user_id: user.id,
      token_hash,
      expires_at: expiresAt,
    });

    return {
      access_token,
      refresh_token,
      user: {
        id: user.id,
        email: user.email,
        display_name: user.display_name,
        role: user.role,
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

    return this.generateAuthResponse(user);
  }

  async login(loginDto: LoginDto): Promise<AuthResponseDto> {
    const { email, password } = loginDto;
    const user = await this.userRepository.findOne({ where: { email } });

    if (!user) {
      throw new UnauthorizedException('Invalid credentials');
    }

    const isPasswordValid = await bcrypt.compare(password, user.password_hash);
    if (!isPasswordValid) {
      throw new UnauthorizedException('Invalid credentials');
    }

    return this.generateAuthResponse(user);
  }

  async logout(user_sub: string): Promise<void> {
    await this.refreshTokenRepository.delete({ user_id: user_sub });
  }

  async refreshToken(token: string): Promise<{ access_token: string }> {
    try {
      const payload = this.jwtService.verify(token, {
        secret: process.env.JWT_REFRESH_SECRET || 'khiemhehe',
      });
      const userId = payload.sub;

      const user = await this.userRepository.findOne({ where: { id: userId } });
      if (!user) {
        throw new NotFoundException('User not found');
      }

      const refreshTokens = await this.refreshTokenRepository.find({
        where: { user_id: userId },
      });

      let isValidToken = false;
      for (const record of refreshTokens) {
        if (record.expires_at < new Date()) continue;
        if (await bcrypt.compare(token, record.token_hash)) {
          isValidToken = true;
          break;
        }
      }

      if (!isValidToken) {
        throw new UnauthorizedException('Invalid refresh token');
      }

      const access_token = this.jwtService.sign(
        { sub: user.id, email: user.email, role: user.role },
        { secret: process.env.JWT_SECRET || 'khiemhehe', expiresIn: '15m' },
      );

      return { access_token };
    } catch {
      throw new UnauthorizedException('Invalid refresh token');
    }
  }

  async sendEmailVerification(userId: string): Promise<{ message: string }> {
    const user = await this.userRepository.findOne({ where: { id: userId } });
    if (!user) throw new NotFoundException('User not found');
    if (user.is_verified) {
      throw new BadRequestException('Email is already verified');
    }

    await this.emailVerificationRepository.delete({ userId });

    const token = randomBytes(32).toString('hex');
    const expiresAt = new Date(Date.now() + 24 * 60 * 60 * 1000); // 24h

    await this.emailVerificationRepository.save({ userId, token, expiresAt });

    this.logger.log(
      `[DEV] Email verification token for ${user.email}: ${token}`,
    );

    return { message: 'Verification email sent. Check your inbox.' };
  }

  async verifyEmail(token: string): Promise<{ message: string }> {
    const record = await this.emailVerificationRepository.findOne({
      where: { token },
    });

    if (!record) throw new BadRequestException('Invalid or expired token');
    if (record.usedAt) throw new BadRequestException('Token already used');
    if (record.expiresAt < new Date())
      throw new BadRequestException('Token expired');

    await this.userRepository.update(record.userId, { is_verified: true });
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

    await this.passwordResetRepository.delete({ userId: user.id });

    const token = randomBytes(32).toString('hex');
    const expiresAt = new Date(Date.now() + 60 * 60 * 1000); // 1 hour

    await this.passwordResetRepository.save({
      userId: user.id,
      token,
      expiresAt,
    });

    this.logger.log(`[DEV] Password reset token for ${email}: ${token}`);

    return { message: 'If that email exists, a reset link has been sent.' };
  }

  async resetPassword(
    token: string,
    newPassword: string,
  ): Promise<{ message: string }> {
    const record = await this.passwordResetRepository.findOne({
      where: { token },
    });

    if (!record) throw new BadRequestException('Invalid or expired token');
    if (record.usedAt) throw new BadRequestException('Token already used');
    if (record.expiresAt < new Date())
      throw new BadRequestException('Token expired');

    const password_hash = await bcrypt.hash(newPassword, 10);
    await this.userRepository.update(record.userId, { password_hash });
    await this.passwordResetRepository.update(record.id, {
      usedAt: new Date(),
    });

    await this.refreshTokenRepository.delete({ user_id: record.userId });

    return { message: 'Password reset successfully. Please log in again.' };
  }
}
