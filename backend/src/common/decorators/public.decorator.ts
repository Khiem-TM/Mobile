import { SetMetadata } from '@nestjs/common';

export const IS_PUBLIC_KEY = 'isPublic'; // Public key

export const Public = () => SetMetadata(IS_PUBLIC_KEY, true); // Decorator to mark routes as public
