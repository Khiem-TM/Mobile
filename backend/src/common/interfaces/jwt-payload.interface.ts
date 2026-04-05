import { UserRole } from '../enums/user-role.enum';

export interface JwtPayload {
  sub: string; // user id
  email: string;
  role: UserRole;
}

// jwt --> là token để system handle authentication, authorization
// payload --> Chứa 3 thông tin trên
