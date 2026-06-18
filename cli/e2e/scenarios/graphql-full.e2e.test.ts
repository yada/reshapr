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
  GITHUB_GRAPHQL_BACKEND,
  GITHUB_GRAPHQL_SERVICE_NAME,
  GITHUB_GRAPHQL_SERVICE_VERSION,
  GITHUB_GRAPHQL_SPEC,
  GITHUB_RAW_USER_TOOL,
  expectedGithubFullToolCount,
  githubFullMinimumToolCount,
} from '../helpers/fixtures.js';
import { mcpUrl, waitForMcpTools } from '../helpers/mcp-client.js';

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

describe('Scenario: Import the full GitHub GraphQL schema', () => {
  let serviceId: string | undefined;

  afterAll(async () => {
    await deleteServiceIfPresent(serviceId);
  });

  test('keeps imported operations and generated MCP tools in parity', async () => {
    await login();
    await deleteServicesByNameVersion(GITHUB_GRAPHQL_SERVICE_NAME, GITHUB_GRAPHQL_SERVICE_VERSION);

    const imported = await runCliJson<ImportResult>(
      'import',
      '-f', GITHUB_GRAPHQL_SPEC,
      '--sn', GITHUB_GRAPHQL_SERVICE_NAME,
      '--sv', GITHUB_GRAPHQL_SERVICE_VERSION,
      '--be', GITHUB_GRAPHQL_BACKEND,
      '-o', 'json',
    );

    const importedServiceId = imported.service.id;
    serviceId = importedServiceId;
    expect(importedServiceId).toBeTruthy();

    const service = await runCliJson<Service>('service', 'get', importedServiceId, '-o', 'json');
    expect(service.id).toBe(importedServiceId);
    expect(service.name).toBe(GITHUB_GRAPHQL_SERVICE_NAME);
    expect(service.version).toBe(GITHUB_GRAPHQL_SERVICE_VERSION);
    expect(service.type).toBe('GRAPHQL');

    const operations = service.operations ?? [];
    const exactCount = expectedGithubFullToolCount();
    if (exactCount !== undefined) {
      expect(operations).toHaveLength(exactCount);
    }
    expect(operations.length).toBeGreaterThanOrEqual(githubFullMinimumToolCount());

    const tools = await waitForMcpTools(mcpUrl(GITHUB_GRAPHQL_SERVICE_NAME, GITHUB_GRAPHQL_SERVICE_VERSION), {
      exact: exactCount,
      minimum: githubFullMinimumToolCount(),
      include: [GITHUB_RAW_USER_TOOL],
    });
    expect(tools).toHaveLength(operations.length);
  });
});
