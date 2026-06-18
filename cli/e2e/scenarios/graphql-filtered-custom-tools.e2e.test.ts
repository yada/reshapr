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
  GITHUB_CUSTOM_EXPECTED_TOOLS,
  GITHUB_CUSTOM_TOOLS,
  GITHUB_CUSTOM_USER_TOOL,
  GITHUB_FILTERED_EXPECTED_TOOLS,
  GITHUB_GRAPHQL_BACKEND,
  GITHUB_GRAPHQL_SERVICE_NAME,
  GITHUB_GRAPHQL_SERVICE_VERSION,
  GITHUB_GRAPHQL_SPEC,
  GITHUB_RAW_USER_TOOL,
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

describe('Scenario: Import filtered GitHub GraphQL operations and attach custom tools', () => {
  let serviceId: string | undefined;

  afterAll(async () => {
    await deleteServiceIfPresent(serviceId);
  });

  test('filters raw GraphQL tools and replaces them with custom tool projection', async () => {
    await login();
    await deleteServicesByNameVersion(GITHUB_GRAPHQL_SERVICE_NAME, GITHUB_GRAPHQL_SERVICE_VERSION);

    const imported = await runCliJson<ImportResult>(
      'import',
      '-f', GITHUB_GRAPHQL_SPEC,
      '--sn', GITHUB_GRAPHQL_SERVICE_NAME,
      '--sv', GITHUB_GRAPHQL_SERVICE_VERSION,
      '--be', GITHUB_GRAPHQL_BACKEND,
      '--io', '["user"]',
      '-o', 'json',
    );

    const importedServiceId = imported.service.id;
    serviceId = importedServiceId;
    expect(importedServiceId).toBeTruthy();

    const service = await runCliJson<Service>('service', 'get', importedServiceId, '-o', 'json');
    expect(service.type).toBe('GRAPHQL');
    expect(service.operations ?? []).toHaveLength(GITHUB_FILTERED_EXPECTED_TOOLS);

    await waitForMcpTools(mcpUrl(GITHUB_GRAPHQL_SERVICE_NAME, GITHUB_GRAPHQL_SERVICE_VERSION), {
      exact: GITHUB_FILTERED_EXPECTED_TOOLS,
      include: [GITHUB_RAW_USER_TOOL],
      exclude: [GITHUB_CUSTOM_USER_TOOL],
    });

    await runCliJson('attach', '-f', GITHUB_CUSTOM_TOOLS, '-o', 'json');

    const customTools = await waitForMcpTools(mcpUrl(GITHUB_GRAPHQL_SERVICE_NAME, GITHUB_GRAPHQL_SERVICE_VERSION), {
      exact: GITHUB_CUSTOM_EXPECTED_TOOLS,
      include: [GITHUB_CUSTOM_USER_TOOL],
      exclude: [GITHUB_RAW_USER_TOOL],
    });
    expect(customTools).toHaveLength(GITHUB_CUSTOM_EXPECTED_TOOLS);
  });
});
