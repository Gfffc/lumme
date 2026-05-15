import { NextFunction, Request, Response } from 'express';
import { verifyJwt, JwtPayload } from './jwt';

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Express {
    interface Request {
      user?: JwtPayload;
    }
  }
}

export function requireAuth(req: Request, res: Response, next: NextFunction) {
  const header = req.headers.authorization;
  if (!header?.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Token ausente' });
  }
  const token = header.slice('Bearer '.length);
  try {
    req.user = verifyJwt(token);
    next();
  } catch {
    return res.status(401).json({ error: 'Token inválido ou expirado' });
  }
}
