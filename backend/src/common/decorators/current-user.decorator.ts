import { createParamDecorator, ExecutionContext } from '@nestjs/common';
import { JwtPayload } from '../interfaces/jwt-payload.interface';

interface RequestWithUser extends Request {
  user: JwtPayload;
}

export const CurrentUser = createParamDecorator(
  (data: unknown, ctx: ExecutionContext): JwtPayload => {
    const req = ctx.switchToHttp().getRequest<RequestWithUser>();
    return req.user;
  },
);

// custom decorator --> Lấy thông tin user từ req(JWT payload)
/*
--> dễ hiểu: thay vì dùng @Req() req --> req.user , thì dùng @CurrentUser() user: JwtPayload
*/
