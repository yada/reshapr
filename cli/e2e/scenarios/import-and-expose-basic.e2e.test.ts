/*
 * Copyright The Reshapr Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { afterAll, describe, expect, test } from 'vitest';
import { login, runCliJson } from '../helpers/cli-runner.js';
import { deleteServiceIfPresent, deleteServicesByNameVersion } from '../helpers/cleanup.js';
import {
  OPEN_METEO_BACKEND,
  OPEN_METEO_EXPECTED_TOOLS,
  OPEN_METEO_SERVICE_NAME,
  OPEN_METEO_SERVICE_VERSION,
  OPEN_METEO_SPEC,
  OPEN_METEO_TOOL_NAME,
} from '../helpers/fixtures.js';
import { callMcpTool, initializeMcp, mcpUrl, waitForMcpTools } from '../helpers/mcp-client.js';

interface Service {
  id: string;
  name: string;
  version: string;
  type: string;
  operations?: unknown[];
}

interface ImportResult {
  service: Service;
}

interface Exposition {
  service?: Service;
  configurationPlan?: {
    backendEndpoint?: string;
  };
}

describe('Scenario: Import and expose an OpenAPI service via MCP', () => {
  let serviceId: string | undefined;

  afterAll(async () => {
    await deleteServiceIfPresent(serviceId);
  });

  test('imports Open-Meteo, exposes it, and serves MCP tools', async () => {
    await login();
    await deleteServicesByNameVersion(OPEN_METEO_SERVICE_NAME, OPEN_METEO_SERVICE_VERSION);

    const imported = await runCliJson<ImportResult>(
      'import',
      '-f', OPEN_METEO_SPEC,
      '--be', OPEN_METEO_BACKEND,
      '-o', 'json',
    );

    const importedServiceId = imported.service.id;
    serviceId = importedServiceId;
    expect(imported.service.name).toBe(OPEN_METEO_SERVICE_NAME);
    expect(importedServiceId).toBeTruthy();

    const service = await runCliJson<Service>('service', 'get', importedServiceId, '-o', 'json');
    expect(service.id).toBe(importedServiceId);
    expect(service.name).toBe(OPEN_METEO_SERVICE_NAME);
    expect(service.version).toBe(OPEN_METEO_SERVICE_VERSION);
    expect(service.type).toBe('REST');
    expect(service.operations ?? []).toHaveLength(OPEN_METEO_EXPECTED_TOOLS);

    const expositions = await runCliJson<Exposition[]>('expo', 'list', '-o', 'json');
    const exposition = expositions.find(item => item.service?.id === importedServiceId);
    expect(exposition).toBeDefined();
    expect(exposition?.configurationPlan?.backendEndpoint).toBe(OPEN_METEO_BACKEND);

    const endpoint = mcpUrl(OPEN_METEO_SERVICE_NAME, OPEN_METEO_SERVICE_VERSION);
    const initialized = await initializeMcp(endpoint);
    expect(initialized.body.result?.serverInfo).toBeDefined();

    const tools = await waitForMcpTools(endpoint, {
      exact: OPEN_METEO_EXPECTED_TOOLS,
      include: [OPEN_METEO_TOOL_NAME],
    });
    expect(tools).toHaveLength(service.operations?.length ?? 0);

    const toolCall = await callMcpTool(endpoint, OPEN_METEO_TOOL_NAME, {
      latitude: 48.8566,
      longitude: 2.3522,
      current_weather: true,
      timezone: 'Europe/Paris',
    });

    expect(toolCall.error).toBeUndefined();
    expect(toolCall.result?.isError).not.toBe(true);
    expect(toolCall.result?.content?.[0]?.text).toEqual(expect.any(String));

    const backendPayload = JSON.parse(toolCall.result.content[0].text);
    expect(backendPayload.latitude).toEqual(expect.any(Number));
    expect(backendPayload.longitude).toEqual(expect.any(Number));
  });
});
