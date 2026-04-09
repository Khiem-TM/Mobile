import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import * as nodemailer from 'nodemailer';

@Injectable()
export class MailerService {
  private readonly logger = new Logger(MailerService.name);
  private transporter: nodemailer.Transporter;

  constructor(private readonly configService: ConfigService) {
    const host = this.configService.get<string>('MAIL_HOST');
    const port = this.configService.get<number>('MAIL_PORT', 587);
    const user = this.configService.get<string>('MAIL_USER');
    const pass = this.configService.get<string>('MAIL_PASS');

    if (host && user && pass) {
      this.transporter = nodemailer.createTransport({
        host,
        port,
        secure: port === 465,
        auth: { user, pass },
      });
    }
  }

  async sendEmailVerification(email: string, token: string): Promise<void> {
    const frontendUrl = this.configService.get<string>('FRONTEND_URL', 'http://localhost:3000');
    const link = `${frontendUrl}/verify-email?token=${token}`;

    if (!this.transporter) {
      this.logger.warn(`[DEV] Email verification token for ${email}: ${token}`);
      this.logger.warn(`[DEV] Verification link: ${link}`);
      return;
    }

    const from = this.configService.get<string>('MAIL_FROM', 'Calories Tracker <noreply@calories-tracker.app>');
    await this.transporter.sendMail({
      from,
      to: email,
      subject: 'Xác minh địa chỉ email của bạn',
      html: `
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
          <h2 style="color: #4CAF50;">Xác minh email</h2>
          <p>Xin chào,</p>
          <p>Bấm vào nút bên dưới để xác minh địa chỉ email của bạn:</p>
          <a href="${link}" style="
            display: inline-block;
            padding: 12px 24px;
            background-color: #4CAF50;
            color: white;
            text-decoration: none;
            border-radius: 6px;
            margin: 16px 0;
          ">Xác minh email</a>
          <p style="color: #666; font-size: 14px;">Link này có hiệu lực trong 24 giờ.</p>
          <p style="color: #666; font-size: 14px;">Nếu bạn không yêu cầu điều này, hãy bỏ qua email này.</p>
        </div>
      `,
    });

    this.logger.log(`Verification email sent to ${email}`);
  }

  async sendPasswordReset(email: string, token: string): Promise<void> {
    const frontendUrl = this.configService.get<string>('FRONTEND_URL', 'http://localhost:3000');
    const link = `${frontendUrl}/reset-password?token=${token}`;

    if (!this.transporter) {
      this.logger.warn(`[DEV] Password reset token for ${email}: ${token}`);
      this.logger.warn(`[DEV] Reset link: ${link}`);
      return;
    }

    const from = this.configService.get<string>('MAIL_FROM', 'Calories Tracker <noreply@calories-tracker.app>');
    await this.transporter.sendMail({
      from,
      to: email,
      subject: 'Đặt lại mật khẩu',
      html: `
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
          <h2 style="color: #2196F3;">Đặt lại mật khẩu</h2>
          <p>Xin chào,</p>
          <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.</p>
          <a href="${link}" style="
            display: inline-block;
            padding: 12px 24px;
            background-color: #2196F3;
            color: white;
            text-decoration: none;
            border-radius: 6px;
            margin: 16px 0;
          ">Đặt lại mật khẩu</a>
          <p style="color: #666; font-size: 14px;">Link này có hiệu lực trong 1 giờ.</p>
          <p style="color: #666; font-size: 14px;">Nếu bạn không yêu cầu điều này, hãy bỏ qua email này. Mật khẩu của bạn sẽ không thay đổi.</p>
        </div>
      `,
    });

    this.logger.log(`Password reset email sent to ${email}`);
  }
}
