import { v2 as cloudinary } from 'cloudinary';
import { ConfigService } from '@nestjs/config';

export const CLOUDINARY = 'CLOUDINARY';

const REQUIRED_ENV_KEYS = [
  'CLOUDINARY_CLOUD_NAME',
  'CLOUDINARY_API_KEY',
  'CLOUDINARY_API_SECRET',
] as const;

function getRequiredCloudinaryEnv(configService: ConfigService) {
  const values = Object.fromEntries(
    REQUIRED_ENV_KEYS.map((key) => [key, configService.get<string>(key)]),
  ) as Record<(typeof REQUIRED_ENV_KEYS)[number], string | undefined>;

  const missing = REQUIRED_ENV_KEYS.filter((key) => !values[key]?.trim());
  if (missing.length > 0) {
    throw new Error(
      `Missing Cloudinary environment variables: ${missing.join(', ')}`,
    );
  }

  return {
    cloudName: values.CLOUDINARY_CLOUD_NAME as string,
    apiKey: values.CLOUDINARY_API_KEY as string,
    apiSecret: values.CLOUDINARY_API_SECRET as string,
  };
}

export const CloudinaryProvider = {
  provide: CLOUDINARY,
  inject: [ConfigService],
  useFactory: (configService: ConfigService) => {
    const { cloudName, apiKey, apiSecret } =
      getRequiredCloudinaryEnv(configService);

    cloudinary.config({
      cloud_name: cloudName,
      api_key: apiKey,
      api_secret: apiSecret,
    });
    return cloudinary;
  },
};
