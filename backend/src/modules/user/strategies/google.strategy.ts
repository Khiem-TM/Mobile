import { Injectable } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { Strategy, VerifyCallback } from 'passport-google-oauth20';

export interface GoogleProfile {
  email: string;
  display_name: string;
  avatar_url: string | null;
  oauth_id: string;
}

@Injectable()
export class GoogleStrategy extends PassportStrategy(Strategy, 'google') {
  constructor() {
    super({
      clientID: process.env.GOOGLE_CLIENT_ID || '',
      clientSecret: process.env.GOOGLE_CLIENT_SECRET || '',
      callbackURL:
        process.env.GOOGLE_CALLBACK_URL ||
        'http://localhost:3005/auth/google/callback',
      scope: ['email', 'profile'],
    });
  }

  validate(
    _accessToken: string,
    _refreshToken: string,
    profile: any,
    done: VerifyCallback,
  ): void {
    const googleProfile: GoogleProfile = {
      email: profile.emails?.[0]?.value ?? '',
      display_name: profile.displayName ?? '',
      avatar_url: profile.photos?.[0]?.value ?? null,
      oauth_id: profile.id,
    };
    done(null, googleProfile);
  }
}
