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
import { describe, test, expect } from 'vitest';
import { getTempHome, runCli, runCliExpectSuccess } from '../helpers/cli-runner.js';
import { GATEWAY_URL } from '../helpers/setup.js';
import * as path from 'node:path';
import * as fs from 'node:fs';

const CONTROL_PLANE = 'http://localhost:5555';
const ARTIFACT_FILE = path.resolve(import.meta.dirname, '../../../dev/open-meteo-openapi.yml');

/**
 * Full end-to-end scenario: login → import OpenAPI → verify service → verify
 * exposition → send a real MCP HTTP request → cleanup → logout.
 *
 * Tests MUST run sequentially because each step depends on the previous one.
 */
describe('Scenario: Import and expose an OpenAPI service via MCP', () => {

  let serviceId: string;

  // ── Step 1 ─────────────────────────────────────────────────────────────────
  test('login with username and password', async () => {
    const result = await runCli(
      'login',
      '-u', 'e2euser',
      '-p', 'e2e-password',
      '-s', CONTROL_PLANE,
      '-k',
    );

    expect(result.exitCode).toBe(0);
    expect(result.stdout + result.stderr).toContain('Login successful');
  });

  // ── Step 2 ─────────────────────────────────────────────────────────────────
  test('import OpenAPI artifact with backend endpoint', async () => {
    const result = await runCli(
      'import',
      '-f', ARTIFACT_FILE,
      '--be', 'https://api.open-meteo.com',
      '-o', 'json',
    );

    expect(result.exitCode).toBe(0);

    // The JSON output should contain the service info
    const output = result.stdout;
    const data = JSON.parse(output);

    expect(data.service).toBeDefined();
    expect(data.service.name).toContain('Open-Meteo');
    serviceId = data.service.id;
    expect(serviceId).toBeTruthy();
  });

  // ── Step 3 ─────────────────────────────────────────────────────────────────
  test('service list returns the imported service', async () => {
    const result = await runCliExpectSuccess('service', 'list', '-o', 'json');

    const services = JSON.parse(result.stdout);
    expect(Array.isArray(services)).toBe(true);

    const found = services.find((s: any) => s.id === serviceId);
    expect(found).toBeDefined();
    expect(found.name).toContain('Open-Meteo');
    expect(found.type).toBe('REST');
  });

  // ── Step 4 ─────────────────────────────────────────────────────────────────
  test('expo list shows an active exposition', async () => {
    const result = await runCliExpectSuccess('expo', 'list', '-o', 'json');

    const expositions = JSON.parse(result.stdout);
    expect(Array.isArray(expositions)).toBe(true);
    expect(expositions.length).toBeGreaterThanOrEqual(1);

    const expo = expositions.find((e: any) => e.service?.id === serviceId);
    expect(expo).toBeDefined();
    expect(expo.configurationPlan.backendEndpoint).toBe('https://api.open-meteo.com');
  });

  // ── Step 5 ─────────────────────────────────────────────────────────────────
  test('MCP endpoint responds to an initialize request', async () => {
    // Build the MCP endpoint URL:  /mcp/{org}/{serviceName}/{serviceVersion}
    const mcpUrl = `${GATEWAY_URL}/mcp/e2eorg/Open-Meteo+APIs/1.0`;

    const response = await fetch(mcpUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        jsonrpc: '2.0',
        id: 1,
        method: 'initialize',
        params: {
          protocolVersion: '2025-03-26',
          capabilities: {},
          clientInfo: { name: 'e2e-test', version: '0.0.1' },
        },
      }),
    });

    expect(response.status).toBe(200);

    const body = await response.json();
    expect(body.jsonrpc).toBe('2.0');
    expect(body.id).toBe(1);
    expect(body.result).toBeDefined();
    expect(body.result.serverInfo).toBeDefined();
  });

  // ── Step 6 (cleanup) ──────────────────────────────────────────────────────
  test('cleanup: delete the imported service', async () => {
    expect(serviceId).toBeTruthy();

    const result = await runCliExpectSuccess('service', 'delete', serviceId, '-f');
    expect(result.exitCode).toBe(0);

    // Verify it's gone
    const listResult = await runCliExpectSuccess('service', 'list', '-o', 'json');
    if (listResult.stdout) {
      const services = JSON.parse(listResult.stdout);
      const found = services.find((s: any) => s.id === serviceId);
      expect(found).toBeUndefined();
    }
  });

  // ── Step 7 (logout) ──────────────────────────────────────────────────────
  test('logout: logout and check no config remains', async () => {
    expect(serviceId).toBeTruthy();

    const result = await runCliExpectSuccess('logout');
    expect(result.exitCode).toBe(0);

    // Verify we no longer have a file at home/.reshapr/config file
    const configPath = path.join(getTempHome(), '.reshapr', 'config');
    const exists = await fs.promises.access(configPath).then(() => true).catch(() => false);
    expect(exists).toBe(false);
  });
});

