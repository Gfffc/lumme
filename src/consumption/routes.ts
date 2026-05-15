import { Router } from 'express';
import { z } from 'zod';
import { requireAuth } from '../auth/middleware';
import { env } from '../config/env';
import { generateMockConsumption } from '../smartthings/mock';
import { prisma } from '../prisma';

const router = Router();
router.use(requireAuth);

const querySchema = z.object({
  from: z.string().datetime(),
  to: z.string().datetime(),
  granularity: z.enum(['hour', 'day', 'month']).default('day'),
});

router.get('/', (req, res, next) => {
  try {
    const q = querySchema.parse(req.query);
    const from = new Date(q.from);
    const to = new Date(q.to);
    const gran = q.granularity.toUpperCase() as 'HOUR' | 'DAY' | 'MONTH';

    if (env.MOCK_SMARTTHINGS) {
      return res.json(generateMockConsumption(from, to, gran));
    }

    // Produção: consulta na tabela consumption_readings agregada
    // (implementação acontece via job que processa device_events)
    return res.json([]);
  } catch (e) {
    next(e);
  }
});

router.get('/device/:id', async (req, res, next) => {
  try {
    const q = querySchema.parse(req.query);
    const from = new Date(q.from);
    const to = new Date(q.to);
    const gran = q.granularity.toUpperCase() as 'HOUR' | 'DAY' | 'MONTH';

    if (env.MOCK_SMARTTHINGS) {
      // Em mock, divide o consumo total por ~7 (número de devices)
      const all = generateMockConsumption(from, to, gran);
      return res.json(all.map(r => ({ ...r, energyKwh: +(r.energyKwh / 7).toFixed(3) })));
    }

    const readings = await prisma.consumptionReading.findMany({
      where: {
        device: { stDeviceId: req.params.id, userId: req.user!.sub },
        periodStart: { gte: from },
        periodEnd: { lte: to },
        granularity: gran,
      },
      orderBy: { periodStart: 'asc' },
    });

    type Reading = {
      periodStart: Date; periodEnd: Date;
      energyKwh: number; estimatedCost: number | null; granularity: string;
    };
    res.json((readings as Reading[]).map((r) => ({
      periodStart: r.periodStart.toISOString(),
      periodEnd: r.periodEnd.toISOString(),
      energyKwh: r.energyKwh,
      estimatedCost: r.estimatedCost,
      granularity: r.granularity,
    })));
  } catch (e) {
    next(e);
  }
});

export default router;
