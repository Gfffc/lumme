import crypto from 'node:crypto';
import { env } from '../config/env';

/**
 * Criptografia simétrica AES-256-GCM para os tokens OAuth da SmartThings.
 *
 * Formato de saída: base64( iv[12] || authTag[16] || ciphertext )
 *
 * IMPORTANTE: se a TOKEN_ENCRYPTION_KEY for trocada, todos os tokens armazenados
 * ficam inutilizáveis. Em produção, mantenha num KMS (AWS KMS, GCP KMS, Vault).
 */

const ALGO = 'aes-256-gcm';
const IV_LEN = 12;
const TAG_LEN = 16;

function getKey(): Buffer {
  return Buffer.from(env.TOKEN_ENCRYPTION_KEY, 'hex');
}

export function encrypt(plaintext: string): string {
  const iv = crypto.randomBytes(IV_LEN);
  const cipher = crypto.createCipheriv(ALGO, getKey(), iv);
  const ciphertext = Buffer.concat([cipher.update(plaintext, 'utf8'), cipher.final()]);
  const tag = cipher.getAuthTag();
  return Buffer.concat([iv, tag, ciphertext]).toString('base64');
}

export function decrypt(payload: string): string {
  const buf = Buffer.from(payload, 'base64');
  const iv = buf.subarray(0, IV_LEN);
  const tag = buf.subarray(IV_LEN, IV_LEN + TAG_LEN);
  const ciphertext = buf.subarray(IV_LEN + TAG_LEN);
  const decipher = crypto.createDecipheriv(ALGO, getKey(), iv);
  decipher.setAuthTag(tag);
  return Buffer.concat([decipher.update(ciphertext), decipher.final()]).toString('utf8');
}
