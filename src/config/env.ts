import { z } from 'zod';
import dotenv from 'dotenv';

dotenv.config();

const schema = z.object({
  PORT: z.coerce.number().default(8080),
  NODE_ENV: z.enum(['development', 'production', 'test']).default('development'),

  DATABASE_URL: z.string().min(1),

  JWT_SECRET: z.string().min(16, 'JWT_SECRET deve ter ao menos 16 caracteres'),
  JWT_EXPIRES_IN: z.string().default('7d'),

  TOKEN_ENCRYPTION_KEY: z
    .string()
    .regex(/^[0-9a-fA-F]{64}$/, 'TOKEN_ENCRYPTION_KEY deve ser 64 caracteres hex (32 bytes)'),

  ST_CLIENT_ID: z.string().default(''),
  ST_CLIENT_SECRET: z.string().default(''),
  ST_REDIRECT_URI: z.string().url().default('http://localhost:8080/api/v1/auth/smartthings/callback'),
  ST_SCOPES: z.string().default('r:devices:* x:devices:* r:locations:*'),

  APP_REDIRECT_URI: z.string().default('lumme://oauth-callback'),

  MOCK_SMARTTHINGS: z
    .string()
    .default('true')
    .transform((v) => v === 'true'),
});

export const env = schema.parse(process.env);
export type Env = typeof env;
