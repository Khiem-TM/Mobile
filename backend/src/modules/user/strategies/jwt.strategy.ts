import { Injectable, UnauthorizedException } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { ExtractJwt, Strategy } from 'passport-jwt';
import { RedisService } from '../../support/redis/redis.service';

export interface JwtPayload {
  sub: string;
  email: string;
  role: string;
  jti?: string;
}

@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy) {
  constructor(private readonly redisService: RedisService) {
    super({
      jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
      ignoreExpiration: false,
      secretOrKey: process.env.JWT_SECRET || 'khiemhehe',
    });
  }

  async validate(payload: JwtPayload) {
    if (!payload.jti) {
      throw new UnauthorizedException('Token is missing revocation id');
    }
    // Check blacklist — tokens added here on logout/password-change until they expire
    const isBlacklisted = await this.redisService.get(`bl:${payload.jti}`);
    if (isBlacklisted) throw new UnauthorizedException('Token has been revoked');
    return { sub: payload.sub, email: payload.email, role: payload.role };
  }
}
