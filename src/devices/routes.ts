import { Router } from 'express';
import { z } from 'zod';
import { prisma } from '../prisma';
import { requireAuth } from '../auth/middleware';
import { getStAccessToken } from '../auth/tokenResolver';
import {
  classifyDevice,
  getDeviceStatus,
  listDevices,
  sendDeviceCommand,
} from '../smartthings/client';
import { StDevice, StDeviceStatus } from '../smartthings/types';

const router = Router();
router.use(requireAuth);

/** Converte dados da SmartThings para o DTO do app. */
function toAppDto(
  device: StDevice,
  status: StDeviceStatus | null,
  roomName?: string | null
) {
  const main = status?.components?.main ?? {};
  const switchVal = (main as any).switch?.switch?.value;
  const powerVal = (main as any).powerMeter?.power?.value;

  return {
    id: device.deviceId,
    label: device.label,
    roomName: roomName ?? null,
    type: classifyDevice(device),
    status: {
      isOn: switchVal === 'on',
      powerWatts: typeof powerVal === 'number' ? powerVal : null,
      online: true,
    },
    capabilities: device.components.flatMap((c) => c.capabilities.map((cap) => cap.id)),
    lastUpdate: new Date().toISOString(),
  };
}

const ROOM_MAP: Record<string, string> = {
  'sala': 'Sala',
  'quarto-casal': 'Quarto',
  'cozinha': 'Cozinha',
  'area-externa': 'Área Externa',
};

/** Converte dispositivo do banco (cache local + demo) para DTO do app. */
function dbDeviceToDto(device: any) {
  return {
    id: device.stDeviceId,
    label: device.label,
    roomName: device.roomName,
    type: device.deviceType,
    status: {
      isOn: device.isOn,
      powerWatts: device.powerWatts,
      online: device.online,
    },
    capabilities: device.capabilities ? JSON.parse(device.capabilities) : [],
    lastUpdate: device.lastSyncAt?.toISOString() ?? new Date().toISOString(),
  };
}

/** GET /devices — lista dispositivos da SmartThings + dispositivos demo do banco. */
router.get('/', async (req, res, next) => {
  try {
    const userId = req.user!.sub;
    const accessToken = await getStAccessToken(userId);
    const devices = await listDevices(accessToken);

    const statuses = await Promise.all(
      devices.map((d) => getDeviceStatus(accessToken, d.deviceId).catch(() => null))
    );

    await Promise.all(
      devices.map((d, i) => {
        const dto = toAppDto(d, statuses[i], ROOM_MAP[d.roomId ?? '']);
        return prisma.device.upsert({
          where: { userId_stDeviceId: { userId, stDeviceId: d.deviceId } },
          create: {
            userId,
            stDeviceId: d.deviceId,
            label: d.label,
            roomName: dto.roomName,
            locationId: d.locationId ?? null,
            deviceType: dto.type,
            capabilities: JSON.stringify(dto.capabilities),
            isOn: dto.status.isOn,
            powerWatts: dto.status.powerWatts,
            online: dto.status.online,
            lastSyncAt: new Date(),
          },
          update: {
            label: d.label,
            roomName: dto.roomName,
            deviceType: dto.type,
            capabilities: JSON.stringify(dto.capabilities),
            isOn: dto.status.isOn,
            powerWatts: dto.status.powerWatts,
            online: dto.status.online,
            lastSyncAt: new Date(),
          },
        });
      })
    );

    // Lista do SmartThings/mock
    const smartThingsDtos = devices.map((d, i) =>
      toAppDto(d, statuses[i], ROOM_MAP[d.roomId ?? ''])
    );

    // Adiciona dispositivos "demo" (criados pelo usuário, prefixo demo-)
    const demoDevices = await prisma.device.findMany({
      where: { userId, stDeviceId: { startsWith: 'demo-' } },
    });
    const demoDtos = demoDevices.map(dbDeviceToDto);

    res.json([...smartThingsDtos, ...demoDtos]);
  } catch (e) {
    next(e);
  }
});

/** GET /devices/:id */
router.get('/:id', async (req, res, next) => {
  try {
    const userId = req.user!.sub;

    // Se for dispositivo demo, busca no banco
    if (req.params.id.startsWith('demo-')) {
      const device = await prisma.device.findFirst({
        where: { userId, stDeviceId: req.params.id },
      });
      if (!device) return res.status(404).json({ error: 'Dispositivo não encontrado' });
      return res.json(dbDeviceToDto(device));
    }

    const accessToken = await getStAccessToken(userId);
    const devices = await listDevices(accessToken);
    const device = devices.find((d) => d.deviceId === req.params.id);
    if (!device) return res.status(404).json({ error: 'Dispositivo não encontrado' });

    const status = await getDeviceStatus(accessToken, device.deviceId);
    res.json(toAppDto(device, status, ROOM_MAP[device.roomId ?? '']));
  } catch (e) {
    next(e);
  }
});

/** POST /devices — adiciona um dispositivo "demo" salvo apenas no banco do backend. */
const createDeviceSchema = z.object({
  label: z.string().min(1).max(120),
  type: z.enum(['SWITCH', 'LIGHT_BULB', 'THERMOSTAT', 'OUTLET', 'SENSOR']),
  roomName: z.string().max(80).optional(),
});

router.post('/', async (req, res, next) => {
  try {
    const body = createDeviceSchema.parse(req.body);
    const userId = req.user!.sub;

    const stDeviceId = `demo-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;

    const device = await prisma.device.create({
      data: {
        userId,
        stDeviceId,
        label: body.label,
        roomName: body.roomName ?? null,
        deviceType: body.type,
        capabilities: JSON.stringify(['switch']),
        isOn: false,
        powerWatts: null,
        online: true,
        lastSyncAt: new Date(),
      },
    });

    res.status(201).json(dbDeviceToDto(device));
  } catch (e) {
    next(e);
  }
});

/** DELETE /devices/:id — só permite remover dispositivos demo. */
router.delete('/:id', async (req, res, next) => {
  try {
    const userId = req.user!.sub;
    if (!req.params.id.startsWith('demo-')) {
      return res.status(403).json({
        error: 'Apenas dispositivos demo podem ser removidos. Para dispositivos SmartThings, use o app oficial.',
      });
    }
    await prisma.device.deleteMany({
      where: { userId, stDeviceId: req.params.id },
    });
    res.status(204).end();
  } catch (e) {
    next(e);
  }
});

/** POST /devices/:id/command */
const commandSchema = z.object({
  capability: z.string().min(1),
  command: z.string().min(1),
  arguments: z.array(z.any()).optional().default([]),
});

router.post('/:id/command', async (req, res, next) => {
  try {
    const body = commandSchema.parse(req.body);
    const userId = req.user!.sub;

    // Dispositivos demo: apenas atualiza o banco
    if (req.params.id.startsWith('demo-')) {
      if (body.capability === 'switch') {
        await prisma.device.updateMany({
          where: { userId, stDeviceId: req.params.id },
          data: { isOn: body.command === 'on' },
        });
      }
      return res.status(204).end();
    }

    // Dispositivos SmartThings: envia comando real
    const accessToken = await getStAccessToken(userId);
    await sendDeviceCommand(
      accessToken,
      req.params.id,
      body.capability,
      body.command,
      body.arguments
    );

    if (body.capability === 'switch') {
      await prisma.device.updateMany({
        where: { userId, stDeviceId: req.params.id },
        data: { isOn: body.command === 'on' },
      });
    }
    res.status(204).end();
  } catch (e) {
    next(e);
  }
});

export default router;
