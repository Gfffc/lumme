import axios from 'axios';
import { env } from '../config/env';
import { StDevice, StDeviceStatus, StTokenResponse } from './types';
import { MOCK_DEVICES, executeMockCommand, getMockStatus } from './mock';

const ST_BASE = 'https://api.smartthings.com';

// =================== OAuth ===================

export interface TokenPair {
  accessToken: string;
  refreshToken: string;
  expiresAt: Date;
  scopes: string[];
}

export async function exchangeAuthCode(code: string): Promise<TokenPair> {
  if (env.MOCK_SMARTTHINGS) {
    return {
      accessToken: 'mock-access-token-' + code,
      refreshToken: 'mock-refresh-token-' + code,
      expiresAt: new Date(Date.now() + 24 * 3600 * 1000),
      scopes: env.ST_SCOPES.split(' '),
    };
  }

  const basicAuth = Buffer.from(`${env.ST_CLIENT_ID}:${env.ST_CLIENT_SECRET}`).toString('base64');
  const params = new URLSearchParams({
    grant_type: 'authorization_code',
    code,
    redirect_uri: env.ST_REDIRECT_URI,
  });

  const { data } = await axios.post<StTokenResponse>(`${ST_BASE}/oauth/token`, params, {
    headers: {
      Authorization: `Basic ${basicAuth}`,
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  });

  return {
    accessToken: data.access_token,
    refreshToken: data.refresh_token,
    expiresAt: new Date(Date.now() + data.expires_in * 1000),
    scopes: data.scope.split(' '),
  };
}

export async function refreshAccessToken(refreshToken: string): Promise<TokenPair> {
  if (env.MOCK_SMARTTHINGS) {
    return {
      accessToken: 'mock-access-token-refreshed',
      refreshToken,
      expiresAt: new Date(Date.now() + 24 * 3600 * 1000),
      scopes: env.ST_SCOPES.split(' '),
    };
  }

  const basicAuth = Buffer.from(`${env.ST_CLIENT_ID}:${env.ST_CLIENT_SECRET}`).toString('base64');
  const params = new URLSearchParams({
    grant_type: 'refresh_token',
    refresh_token: refreshToken,
  });

  const { data } = await axios.post<StTokenResponse>(`${ST_BASE}/oauth/token`, params, {
    headers: {
      Authorization: `Basic ${basicAuth}`,
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  });

  return {
    accessToken: data.access_token,
    refreshToken: data.refresh_token,
    expiresAt: new Date(Date.now() + data.expires_in * 1000),
    scopes: data.scope.split(' '),
  };
}

// =================== Devices ===================

export async function listDevices(accessToken: string): Promise<StDevice[]> {
  if (env.MOCK_SMARTTHINGS) return MOCK_DEVICES;

  const { data } = await axios.get<{ items: StDevice[] }>(`${ST_BASE}/v1/devices`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  return data.items;
}

export async function getDeviceStatus(
  accessToken: string,
  stDeviceId: string
): Promise<StDeviceStatus> {
  if (env.MOCK_SMARTTHINGS) return getMockStatus(stDeviceId);

  const { data } = await axios.get<StDeviceStatus>(
    `${ST_BASE}/v1/devices/${stDeviceId}/status`,
    { headers: { Authorization: `Bearer ${accessToken}` } }
  );
  return data;
}

export async function sendDeviceCommand(
  accessToken: string,
  stDeviceId: string,
  capability: string,
  command: string,
  args: unknown[] = []
): Promise<void> {
  if (env.MOCK_SMARTTHINGS) {
    executeMockCommand(stDeviceId, capability, command);
    return;
  }

  await axios.post(
    `${ST_BASE}/v1/devices/${stDeviceId}/commands`,
    {
      commands: [
        { component: 'main', capability, command, arguments: args },
      ],
    },
    { headers: { Authorization: `Bearer ${accessToken}` } }
  );
}

// =================== Helpers ===================

export function buildAuthorizeUrl(state: string): string {
  if (env.MOCK_SMARTTHINGS) {
    // Em mock, manda direto para nosso próprio callback com um code falso
    const params = new URLSearchParams({
      code: 'mock-code-' + Date.now(),
      state,
    });
    return `${env.ST_REDIRECT_URI}?${params}`;
  }

  const params = new URLSearchParams({
    client_id: env.ST_CLIENT_ID,
    response_type: 'code',
    redirect_uri: env.ST_REDIRECT_URI,
    scope: env.ST_SCOPES,
    state,
  });
  return `${ST_BASE}/oauth/authorize?${params}`;
}

/** Traduz o tipo SmartThings para nosso enum do app. */
export function classifyDevice(device: StDevice): string {
  const caps = device.components.flatMap((c) => c.capabilities.map((x) => x.id));
  if (caps.includes('motionSensor') || caps.includes('contactSensor') || caps.includes('battery')) return 'SENSOR';
  if (caps.includes('thermostatCoolingSetpoint') || caps.includes('airConditionerMode')) return 'THERMOSTAT';
  if (caps.includes('switchLevel') || caps.includes('colorControl')) return 'LIGHT_BULB';
  if (caps.includes('switch') && caps.includes('powerMeter')) return 'OUTLET';
  if (caps.includes('switch')) return 'SWITCH';
  return 'UNKNOWN';
}
