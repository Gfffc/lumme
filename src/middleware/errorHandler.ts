import { NextFunction, Request, Response } from 'express';
import { ZodError } from 'zod';

export function errorHandler(
  err: unknown,
  _req: Request,
  res: Response,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  _next: NextFunction
) {
  if (err instanceof ZodError) {
    return res.status(400).json({
      error: 'Validação falhou',
      issues: err.issues.map((i) => ({ path: i.path.join('.'), message: i.message })),
    });
  }

  const message = err instanceof Error ? err.message : 'Erro desconhecido';
  const status = /não encontrado|not found/i.test(message) ? 404 : 500;

  console.error('[error]', err);

  res.status(status).json({ error: message });
}
