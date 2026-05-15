import { StDevice, StDeviceStatus } from './types';

/**
 * Dispositivos simulados de uma casa com 2 quartos, sala, cozinha e área externa.
 * Os tipos escolhidos cobrem os principais cenários do app.
 */
export const MOCK_DEVICES: StDevice[] = [
  {
    deviceId: 'mock-lamp-living',
    label: 'Luz da Sala',
    roomId: 'sala',
    locationId: 'home',
    deviceTypeName: 'LIGHT_BULB',
    components: [
      { id: 'main', capabilities: [{ id: 'switch', version: 1 }, { id: 'switchLevel', version: 1 }, { id: 'powerMeter', version: 1 }] },
    ],
  },
  {
    deviceId: 'mock-tv-living',
    label: 'TV da Sala',
    roomId: 'sala',
    locationId: 'home',
    deviceTypeName: 'OUTLET',
    components: [
      { id: 'main', capabilities: [{ id: 'switch', version: 1 }, { id: 'powerMeter', version: 1 }] },
    ],
  },
  {
    deviceId: 'mock-ac-bedroom',
    label: 'Ar-condicionado Quarto',
    roomId: 'quarto-casal',
    locationId: 'home',
    deviceTypeName: 'THERMOSTAT',
    components: [
      { id: 'main', capabilities: [{ id: 'switch', version: 1 }, { id: 'thermostatCoolingSetpoint', version: 1 }, { id: 'powerMeter', version: 1 }] },
    ],
  },
  {
    deviceId: 'mock-lamp-bedroom',
    label: 'Luz do Quarto',
    roomId: 'quarto-casal',
    locationId: 'home',
    deviceTypeName: 'LIGHT_BULB',
    components: [
      { id: 'main', capabilities: [{ id: 'switch', version: 1 }, { id: 'switchLevel', version: 1 }] },
    ],
  },
  {
    deviceId: 'mock-fridge',
    label: 'Geladeira',
    roomId: 'cozinha',
    locationId: 'home',
    deviceTypeName: 'OUTLET',
    components: [
      { id: 'main', capabilities: [{ id: 'switch', version: 1 }, { id: 'powerMeter', version: 1 }] },
    ],
  },
  {
    deviceId: 'mock-microwave',
    label: 'Micro-ondas',
    roomId: 'cozinha',
    locationId: 'home',
    deviceTypeName: 'OUTLET',
    components: [
      { id: 'main', capabilities: [{ id: 'switch', version: 1 }, { id: 'powerMeter', version: 1 }] },
    ],
  },
  {
    deviceId: 'mock-motion-sensor',
    label: 'Sensor de Presença',
    roomId: 'quarto-casal',
    locationId: 'home',
    deviceTypeName: 'SENSOR',
    components: [
      { id: 'main', capabilities: [{ id: 'motionSensor', version: 1 }, { id: 'battery', version: 1 }] },
    ],
  },
];

// Estado em memória — simula que os comandos têm efeito
const deviceState: Record<string, { isOn: boolean; powerWatts: number }> = {
  'mock-lamp-living':    { isOn: true,  powerWatts: 12 },
  'mock-tv-living':      { isOn: true,  powerWatts: 85 },
  'mock-ac-bedroom':     { isOn: false, powerWatts: 0 },
  'mock-lamp-bedroom':   { isOn: false, powerWatts: 0 },
  'mock-fridge':         { isOn: true,  powerWatts: 140 },
  'mock-microwave':      { isOn: false, powerWatts: 0 },
  'mock-motion-sensor':  { isOn: false, powerWatts: 0 },
};

export function getMockStatus(deviceId: string): StDeviceStatus {
  const s = deviceState[deviceId] ?? { isOn: false, powerWatts: 0 };
  return {
    components: {
      main: {
        switch: { switch: { value: s.isOn ? 'on' : 'off' } },
        powerMeter: { power: { value: s.powerWatts, unit: 'W' } },
      },
    },
  };
}

export function executeMockCommand(deviceId: string, capability: string, command: string): boolean {
  const s = deviceState[deviceId];
  if (!s) return false;
  if (capability === 'switch') {
    s.isOn = command === 'on';
    // Simulação: quando liga, puxa uma potência coerente com o tipo
    if (s.isOn) {
      s.powerWatts = ({
        'mock-lamp-living': 12, 'mock-lamp-bedroom': 8,
        'mock-tv-living': 85, 'mock-ac-bedroom': 1100,
        'mock-fridge': 140, 'mock-microwave': 1500,
      } as Record<string, number>)[deviceId] ?? 10;
    } else {
      s.powerWatts = deviceId === 'mock-fridge' ? 140 : 0; // geladeira nunca desliga de fato
    }
  }
  return true;
}

/** Gera consumo simulado coerente com a granularidade pedida. */
export function generateMockConsumption(
  from: Date,
  to: Date,
  granularity: 'HOUR' | 'DAY' | 'MONTH'
) {
  const stepMs = granularity === 'HOUR' ? 3_600_000
              : granularity === 'DAY'  ? 86_400_000
              : 2_592_000_000;

  const readings: Array<{
    periodStart: string;
    periodEnd: string;
    energyKwh: number;
    estimatedCost: number;
    granularity: string;
  }> = [];

  const PRICE_PER_KWH = 0.78; // R$/kWh — tarifa convencional média ANEEL

  for (let t = from.getTime(); t < to.getTime(); t += stepMs) {
    const start = new Date(t);
    const end = new Date(Math.min(t + stepMs, to.getTime()));

    // Perfil diário: mais consumo à noite (19h-23h), pico aos fins de semana
    const hour = start.getHours();
    const isPeak = hour >= 19 && hour <= 23;
    const isWeekend = start.getDay() === 0 || start.getDay() === 6;
    const base = granularity === 'HOUR' ? 0.35 : granularity === 'DAY' ? 8.5 : 250;
    const variation = (Math.random() * 0.4 + 0.8);
    const kwh = base * variation * (isPeak ? 1.4 : 1.0) * (isWeekend ? 1.15 : 1.0);

    readings.push({
      periodStart: start.toISOString(),
      periodEnd: end.toISOString(),
      energyKwh: +kwh.toFixed(3),
      estimatedCost: +(kwh * PRICE_PER_KWH).toFixed(2),
      granularity,
    });
  }
  return readings;
}
