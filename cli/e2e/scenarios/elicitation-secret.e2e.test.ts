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
import {
  deleteSecretIfPresent,
  deleteSecretsByName,
  deleteServiceIfPresent,
  deleteServicesByNameVersion,
} from '../helpers/cleanup.js';
import {
  GITHUB_ELICITATION_LOGIN,
  GITHUB_ELICITATION_SECRET_NAME,
  GITHUB_FILTERED_EXPECTED_TOOLS,
  GITHUB_GRAPHQL_BACKEND,
  GITHUB_GRAPHQL_SERVICE_NAME,
  GITHUB_GRAPHQL_SERVICE_VERSION,
  GITHUB_GRAPHQL_SPEC,
  GITHUB_RAW_USER_TOOL,
} from '../helpers/fixtures.js';
import { callMcpTool, initializeMcp, mcpUrl, waitForMcpTools } from '../helpers/mcp-client.js';
import { waitForHttpOk } from '../helpers/ui-smoke.js';

interface Service {
  id: string;
}

interface Secret {
  id: string;
  name: string;
}

interface ImportResult {
  service: Service;
}

describe('Scenario: GitHub GraphQL URL elicitation for backend credentials', () => {
  let serviceId: string | undefined;
  let secretId: string | undefined;

  afterAll(async () => {
    await deleteServiceIfPresent(serviceId);
    await deleteSecretIfPresent(secretId);
  });

  test('returns a URL elicitation form when a backend token is required', async () => {
    await login();
    await deleteServicesByNameVersion(GITHUB_GRAPHQL_SERVICE_NAME, GITHUB_GRAPHQL_SERVICE_VERSION);
    await deleteSecretsByName(GITHUB_ELICITATION_SECRET_NAME);

    const secret = await runCliJson<Secret>(
      'secret',
      'create-elicitation',
      GITHUB_ELICITATION_SECRET_NAME,
      '-t', 'Authorization',
      '-d', 'GitHub token elicitation e2e',
      '-o', 'json',
    );
    const createdSecretId = secret.id;
    secretId = createdSecretId;
    expect(secret.name).toBe(GITHUB_ELICITATION_SECRET_NAME);

    const imported = await runCliJson<ImportResult>(
      'import',
      '-f', GITHUB_GRAPHQL_SPEC,
      '--sn', GITHUB_GRAPHQL_SERVICE_NAME,
      '--sv', GITHUB_GRAPHQL_SERVICE_VERSION,
      '--be', GITHUB_GRAPHQL_BACKEND,
      '--io', '["user"]',
      '--bs', createdSecretId,
      '-o', 'json',
    );
    serviceId = imported.service.id;

    const endpoint = mcpUrl(GITHUB_GRAPHQL_SERVICE_NAME, GITHUB_GRAPHQL_SERVICE_VERSION);
    await waitForMcpTools(endpoint, {
      exact: GITHUB_FILTERED_EXPECTED_TOOLS,
      include: [GITHUB_RAW_USER_TOOL],
    });

    const initialized = await initializeMcp(endpoint, { elicitation: {} });
    expect(initialized.sessionId).toBeTruthy();

    const response = await callMcpTool(endpoint, GITHUB_RAW_USER_TOOL, { login: GITHUB_ELICITATION_LOGIN }, initialized.sessionId);
    expect(response.error?.code).toBe(-32042);

    const elicitation = response.error?.data?.elicitations?.[0];
    expect(elicitation?.mode).toBe('url');
    expect(elicitation?.elicitationId).toEqual(expect.any(String));
    expect(elicitation?.url).toEqual(expect.stringContaining('/elicitation/form?elicitationId='));

    const form = await waitForHttpOk(elicitation.url);
    const html = (await form.text()).toLowerCase();
    expect(html).toContain('reshapr elicitation');
    expect(html).toContain('action="/elicitation/complete"');
    expect(html).toContain('name="elicitationid"');
    expect(html).toContain('id="tokenvalue"');
    expect(html).not.toContain('was not found');
  });
});
