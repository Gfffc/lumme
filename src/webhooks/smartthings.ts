import { Router, Request, Response } from 'express';
import { prisma } from '../prisma';

const router = Router();

/**
 * Endpoint para receber eventos da SmartThings.
 *
 * IMPORTANTE em produção:
 * 1) Validar assinatura HTTP Signatures (RSA-SHA256) com a chave pública da Samsung.
 *    Veja: https://developer.smartthings.com/docs/enterprise/enterprise-api-overview/eventing/authorization
 * 2) Responder em < 5s (a Samsung retenta em caso de timeout → precisa ser idempotente).
 * 3) Aceitar o handshake de confirmação na primeira vez (SINK_CONFIRMATION).
 */
router.post('/smartthings', async (req: Request, res: Response) => {
  const body = req.body;

  // Handshake: Samsung envia um challenge que precisamos ecoar
  if (body?.notificationType === 'SINK_CONFIRMATION') {
    const challenge = body?.sinkConfirmationNotification?.challenge;
    return res.json({ sinkConfirmationNotification: { challenge } });
  }

  // Processamento do evento
  try {
    const events = body?.events ?? [];
    for (const ev of events) {
      const stDeviceId = ev.deviceEvent?.deviceId;
      if (!stDeviceId) continue;

      const device = await prisma.device.findFirst({
        where: { stDeviceId },
      });
      if (!device) continue;

      await prisma.deviceEvent.create({
        data: {
          deviceId: device.id,
          capability: ev.deviceEvent.capability ?? 'unknown',
          attribute: ev.deviceEvent.attribute ?? 'unknown',
          value: JSON.stringify(ev.deviceEvent.value ?? null),
          eventTime: new Date(ev.deviceEvent.eventTime ?? Date.now()),
        },
      });

      // Atualização rápida do estado no cache local
      if (ev.deviceEvent.capability === 'switch' && ev.deviceEvent.attribute === 'switch') {
        await prisma.device.update({
          where: { id: device.id },
          data: { isOn: ev.deviceEvent.value?.value === 'on' },
        });
      }
      if (ev.deviceEvent.capability === 'powerMeter' && ev.deviceEvent.attribute === 'power') {
        await prisma.device.update({
          where: { id: device.id },
          data: { powerWatts: Number(ev.deviceEvent.value?.value) || null },
        });
      }
    }
    res.status(200).json({ ok: true });
  } catch (e) {
    // Devolvemos 200 para evitar retries em falhas nossas (logamos e seguimos)
    console.error('[webhook] erro:', e);
    res.status(200).json({ ok: true, warning: 'processed with errors' });
  }
});

export default router;
