import { Router } from 'express';
import crypto from 'node:crypto';
import { env } from '../config/env';
import { prisma } from '../prisma';
import { signJwt } from './jwt';
import { encrypt } from '../crypto/tokenCipher';
import { buildAuthorizeUrl, exchangeAuthCode } from '../smartthings/client';
import { renderDeepLinkRedirect } from './deepLinkPage';

const router = Router();

// Guarda temporária dos "state" do OAuth (em produção: Redis com TTL de 10min)
const oauthStates = new Map<string, { createdAt: number }>();
setInterval(() => {
  const tenMinAgo = Date.now() - 600_000;
  for (const [k, v] of oauthStates) if (v.createdAt < tenMinAgo) oauthStates.delete(k);
}, 60_000);

/**
 * 1) App chama isto → recebe a URL para abrir no Chrome Custom Tab.
 */
router.post('/smartthings/start', (_req, res) => {
  const state = crypto.randomUUID();
  oauthStates.set(state, { createdAt: Date.now() });

  const authorizeUrl = buildAuthorizeUrl(state);
  res.json({ authorizeUrl, state });
});

/**
 * 2) Samsung redireciona o navegador para cá com ?code=...&state=...
 *    Trocamos o code por tokens, salvamos e redirecionamos para o deep link do app.
 */
router.get('/smartthings/callback', async (req, res) => {
  const code = String(req.query.code ?? '');
  const state = String(req.query.state ?? '');
  const errorParam = req.query.error ? String(req.query.error) : null;

  if (errorParam) {
    return renderDeepLinkRedirect(
      res,
      `${env.APP_REDIRECT_URI}?error=${encodeURIComponent(errorParam)}`,
      { title: 'Autorização negada', message: 'Voltando ao aplicativo...' }
    );
  }
  if (!code || !state || !oauthStates.has(state)) {
    return renderDeepLinkRedirect(
      res,
      `${env.APP_REDIRECT_URI}?error=invalid_state`,
      { title: 'Erro na autorização', message: 'Voltando ao aplicativo...' }
    );
  }
  oauthStates.delete(state);

  try {
    const tokens = await exchangeAuthCode(code);

    // Em modo mock, cada login cria um usuário fake diferente
    const fakeEmail = env.MOCK_SMARTTHINGS
      ? `mock-${Date.now()}@lumme.dev`
      : 'user@smartthings.com'; // em prod, buscaríamos via /myprofile

    const user = await prisma.user.upsert({
      where: { email: fakeEmail },
      create: { email: fakeEmail, name: 'Usuário Lumme' },
      update: {},
    });

    await prisma.smartThingsAccount.upsert({
      where: { userId: user.id },
      create: {
        userId: user.id,
        accessTokenEncrypted: encrypt(tokens.accessToken),
        refreshTokenEncrypted: encrypt(tokens.refreshToken),
        tokenExpiresAt: tokens.expiresAt,
        scopes: tokens.scopes.join(','),
      },
      update: {
        accessTokenEncrypted: encrypt(tokens.accessToken),
        refreshTokenEncrypted: encrypt(tokens.refreshToken),
        tokenExpiresAt: tokens.expiresAt,
        scopes: tokens.scopes.join(','),
      },
    });

    // Passamos um "short-lived code" pro app trocar por JWT.
    // Simplificação: vamos passar um code-assinado curto.
    const { token } = signJwt({ sub: user.id, email: user.email });

    // URL final: lumme://oauth-callback?code=<jwt>&state=<state>
    return renderDeepLinkRedirect(
      res,
      `${env.APP_REDIRECT_URI}?code=${token}&state=${state}`
    );
  } catch (e: any) {
    console.error('[oauth/callback] erro:', e.message);
    return renderDeepLinkRedirect(
      res,
      `${env.APP_REDIRECT_URI}?error=server_error`,
      { title: 'Erro no servidor', message: 'Voltando ao aplicativo...' }
    );
  }
});

/**
 * 3) App recebe o deep link e chama isto com o "code" (que é nosso JWT)
 *    para validar e receber de volta como "jwt" oficial.
 */
router.post('/smartthings/exchange', async (req, res) => {
  const { code } = req.body ?? {};
  if (typeof code !== 'string') {
    return res.status(400).json({ error: 'Parâmetro "code" obrigatório' });
  }
  // O "code" aqui é o JWT que geramos no callback. Apenas devolvemos com TTL.
  // Em arquiteturas mais robustas, o code seria de uso único e distinto do JWT.
  try {
    const { verifyJwt } = await import('./jwt.js');
    const payload = verifyJwt(code);
    const { token, expiresAt } = signJwt({ sub: payload.sub, email: payload.email });
    return res.json({ jwt: token, expiresAt: expiresAt.toISOString() });
  } catch {
    return res.status(401).json({ error: 'Code inválido' });
  }
});

/**
 * Renovação do JWT (usa o próprio JWT atual).
 */
router.post('/refresh', async (req, res) => {
  const header = req.headers.authorization;
  if (!header?.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Token ausente' });
  }
  try {
    const { verifyJwt } = await import('./jwt.js');
    const payload = verifyJwt(header.slice(7));
    const { token, expiresAt } = signJwt({ sub: payload.sub, email: payload.email });
    return res.json({ jwt: token, expiresAt: expiresAt.toISOString() });
  } catch {
    return res.status(401).json({ error: 'Token inválido' });
  }
});

export default router;

