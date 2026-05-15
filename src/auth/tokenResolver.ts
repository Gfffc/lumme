import { prisma } from '../prisma';
import { decrypt, encrypt } from '../crypto/tokenCipher';
import { refreshAccessToken } from '../smartthings/client';

/**
 * Busca um access token válido para o usuário. Faz refresh automático
 * se estiver expirando nas próximas 5 minutos.
 */
export async function getStAccessToken(userId: string): Promise<string> {
  const account = await prisma.smartThingsAccount.findUnique({ where: { userId } });
  if (!account) throw new Error('Conta SmartThings não conectada');

  const expiresInMs = account.tokenExpiresAt.getTime() - Date.now();
  if (expiresInMs > 5 * 60 * 1000) {
    return decrypt(account.accessTokenEncrypted);
  }

  // Token prestes a expirar → renova
  const newPair = await refreshAccessToken(decrypt(account.refreshTokenEncrypted));
  await prisma.smartThingsAccount.update({
    where: { userId },
    data: {
      accessTokenEncrypted: encrypt(newPair.accessToken),
      refreshTokenEncrypted: encrypt(newPair.refreshToken),
      tokenExpiresAt: newPair.expiresAt,
    },
  });
  return newPair.accessToken;
}
