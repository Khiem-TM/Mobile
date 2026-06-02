import { ForbiddenException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { AuthService } from './auth.service';
import { User } from '../entities/user.entity';

describe('AuthService admin-compatible token generation', () => {
  const OLD_ENV = process.env;

  beforeEach(() => {
    jest.resetModules();
    process.env = {
      ...OLD_ENV,
      JWT_SECRET: 'test-jwt-secret',
      JWT_REFRESH_SECRET: 'test-refresh-secret',
    };
  });

  afterAll(() => {
    process.env = OLD_ENV;
  });

  function createService() {
    const refreshTokenRepository = { save: jest.fn() };
    const redisService = { set: jest.fn() };
    const service = new AuthService(
      {} as any,
      refreshTokenRepository as any,
      {} as any,
      {} as any,
      new JwtService(),
      {} as any,
      {} as any,
      redisService as any,
    );
    return { service, refreshTokenRepository, redisService };
  }

  it('generates access tokens with jti so JwtStrategy accepts them', async () => {
    const { service, refreshTokenRepository, redisService } = createService();
    const user = {
      id: 'user-1',
      email: 'admin@test.com',
      display_name: 'Admin',
      avatar_url: null,
      role: 'admin',
      is_verified: true,
      is_active: true,
    } as User;

    const response = await service.generateAuthResponse(user);
    const decoded = new JwtService().decode(response.access_token) as { jti?: string; role?: string };

    expect(decoded.jti).toBeTruthy();
    expect(decoded.role).toBe('admin');
    expect(response).toHaveProperty('refresh_token');
    expect(redisService.set).toHaveBeenCalled();
    expect(refreshTokenRepository.save).toHaveBeenCalled();
  });

  it('rejects inactive users before issuing tokens', async () => {
    const { service } = createService();
    const user = {
      id: 'user-1',
      email: 'admin@test.com',
      display_name: 'Admin',
      role: 'admin',
      is_active: false,
    } as User;

    await expect(service.generateAuthResponse(user)).rejects.toBeInstanceOf(ForbiddenException);
  });
});
