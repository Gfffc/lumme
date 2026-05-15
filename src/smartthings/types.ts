export interface StTokenResponse {
  access_token: string;
  refresh_token: string;
  expires_in: number;
  scope: string;
  token_type: string;
}

export interface StDevice {
  deviceId: string;
  label: string;
  roomId?: string;
  locationId?: string;
  deviceTypeName?: string;
  components: Array<{
    id: string;
    capabilities: Array<{ id: string; version: number }>;
  }>;
}

export interface StDeviceList {
  items: StDevice[];
}

export interface StDeviceStatus {
  components: {
    [componentId: string]: {
      [capability: string]: {
        [attribute: string]: {
          value: unknown;
          unit?: string;
          timestamp?: string;
        };
      };
    };
  };
}
