import {
  Injectable,
  NestInterceptor,
  ExecutionContext,
  CallHandler,
  HttpStatus,
} from '@nestjs/common';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export interface ApiResponse<T> {
  success: boolean;
  statusCode: number;
  data: T;
  message?: string;
}

@Injectable()
export class ResponseInterceptor<T> implements NestInterceptor<
  T,
  ApiResponse<T>
> {
  intercept(
    context: ExecutionContext,
    next: CallHandler,
  ): Observable<ApiResponse<T>> {
    const response = context.switchToHttp().getResponse();
    const statusCode: number = response.statusCode ?? HttpStatus.OK;

    return next.handle().pipe(
      map((data) => {
        // If the handler already returned a structured ApiResponse, don't wrap again
        if (
          data &&
          typeof data === 'object' &&
          'success' in data &&
          'data' in data
        ) {
          return data;
        }
        return {
          success: true,
          statusCode,
          data,
        };
      }),
    );
  }
}
