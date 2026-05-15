import express from 'express';
import cors from 'cors';
import authRoutes from './auth/routes';
import deviceRoutes from './devices/routes';
import consumptionRoutes from './consumption/routes';
import goalRoutes from './goals/routes';
import webhookRoutes from './webhooks/smartthings';
import { errorHandler } from './middleware/errorHandler';

export function createApp() {
  const app = express();

  app.use(cors());
  app.use(express.json({ limit: '1mb' }));

  // Health check
  app.get('/health', (_req, res) => {
    res.json({ status: 'ok', timestamp: new Date().toISOString() });
  });

  // Rotas v1
  app.use('/api/v1/auth', authRoutes);
  app.use('/api/v1/devices', deviceRoutes);
  app.use('/api/v1/consumption', consumptionRoutes);
  app.use('/api/v1/goals', goalRoutes);
  app.use('/api/v1/webhooks', webhookRoutes);

  // 404
  app.use((_req, res) => res.status(404).json({ error: 'Rota não encontrada' }));

  // Erros
  app.use(errorHandler);

  return app;
}
