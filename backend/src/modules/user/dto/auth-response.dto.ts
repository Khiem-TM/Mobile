export class AuthResponseDto {
  access_token!: string;
  refresh_token!: string;
  token_type!: string;
  expires_in!: number;
  user!: {
    id: string;
    email: string;
    display_name: string;
    avatar_url: string | null;
    role: string;
    is_verified: boolean;
  };
}
