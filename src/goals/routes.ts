import { Router } from 'express';
import { z } from 'zod';
import { prisma } from '../prisma';
import { requireAuth } from '../auth/middleware';
import { env } from '../config/env';
import { generateMockConsumption } from '../smartthings/mock';

const router = Router();
router.use(requireAuth);

/** Calcula o consumo acumulado da meta no período corrente. */
async function computeProgressKwh(userId: string, goal: {
  period: string; startsAt: Date; endsAt: Date | null;
}): Promise<number> {
  const now = new Date();
  const from = goal.startsAt;
  const to = goal.endsAt ?? now;

  if (env.MOCK_SMARTTHINGS) {
    // Soma o consumo mockado até agora
    const daysElapsed = Math.max(1, Math.ceil((now.getTime() - from.getTime()) / 86_400_000));
    const readings = generateMockConsumption(from, now, 'DAY');
    return +readings.reduce((s, r) => s + r.energyKwh, 0).toFixed(2);
  }

  const agg = await prisma.consumptionReading.aggregate({
    where: {
      device: { userId },
      periodStart: { gte: from },
      periodEnd: { lte: to },
    },
    _sum: { energyKwh: true },
  });
  return agg._sum.energyKwh ?? 0;
}

/** GET /goals/active — meta ativa atual. */
router.get('/active', async (req, res, next) => {
  try {
    const userId = req.user!.sub;
    const goal = await prisma.goal.findFirst({
      where: { userId, isActive: true },
      orderBy: { createdAt: 'desc' },
    });
    if (!goal) return res.json(null);

    const progressKwh = await computeProgressKwh(userId, goal);
    res.json({
      id: goal.id,
      targetKwh: goal.targetKwh,
      period: goal.period,
      progressKwh,
    });
  } catch (e) {
    next(e);
  }
});

/** POST /goals — cria/atualiza meta (desativa as anteriores). */
const createGoalSchema = z.object({
  targetKwh: z.number().positive(),
  period: z.enum(['WEEKLY', 'MONTHLY']).default('MONTHLY'),
});

router.post('/', async (req, res, next) => {
  try {
    const body = createGoalSchema.parse(req.body);
    const userId = req.user!.sub;

    // Desativa metas anteriores
    await prisma.goal.updateMany({
      where: { userId, isActive: true },
      data: { isActive: false },
    });

    const now = new Date();
    const endsAt = new Date(now);
    if (body.period === 'WEEKLY') endsAt.setDate(endsAt.getDate() + 7);
    else endsAt.setMonth(endsAt.getMonth() + 1);

    const goal = await prisma.goal.create({
      data: {
        userId,
        targetKwh: body.targetKwh,
        period: body.period,
        startsAt: now,
        endsAt,
        isActive: true,
      },
    });

    const progressKwh = await computeProgressKwh(userId, goal);
    res.status(201).json({
      id: goal.id,
      targetKwh: goal.targetKwh,
      period: goal.period,
      progressKwh,
    });
  } catch (e) {
    next(e);
  }
});

export default router;
