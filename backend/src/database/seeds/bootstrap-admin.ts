// @ts-nocheck
import { NestFactory } from '@nestjs/core';
import { getRepositoryToken } from '@nestjs/typeorm';
import * as bcrypt from 'bcrypt';
import { AppModule } from '../../app.module';
import { User } from '../../modules/user/entities/user.entity';
import { UserRole } from '../../common/enums/user-role.enum';

async function bootstrap() {
  const email = (process.env.ADMIN_EMAIL || 'admin@gmail.com').toLowerCase().trim();
  const password = process.env.ADMIN_PASSWORD || 'admin';
  const displayName = process.env.ADMIN_DISPLAY_NAME || 'Administrator';

  if (!email || !password) {
    throw new Error('ADMIN_EMAIL and ADMIN_PASSWORD are required');
  }

  const app = await NestFactory.createApplicationContext(AppModule);
  const userRepo = app.get(getRepositoryToken(User));

  let admin = await userRepo.findOne({ where: { email } });
  const password_hash = await bcrypt.hash(password, 10);

  if (admin) {
    admin.role = UserRole.ADMIN as User['role'];
    admin.is_active = true;
    admin.is_verified = true;
    admin.display_name = admin.display_name || displayName;
    admin.password_hash = password_hash;
  } else {
    admin = userRepo.create({
      email,
      password_hash,
      display_name: displayName,
      role: UserRole.ADMIN as User['role'],
      is_active: true,
      is_verified: true,
    });
  }

  const saved = await userRepo.save(admin);
  console.log(`Admin ready: ${saved.email} (${saved.id})`);
  await app.close();
}

bootstrap().catch((err) => {
  console.error(err);
  process.exit(1);
});
