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
import { describe, expect, test } from 'vitest';
import { UI_URL } from '../helpers/setup.js';
import { waitForHttpOk } from '../helpers/ui-smoke.js';

interface BootstrapConfiguration {
  mode: string;
  version: string;
  buildTimestamp: string;
  oidcEnabled: boolean;
}

describe('Scenario: Web UI smoke', () => {
  test('serves the SvelteKit app shell and proxies bootstrap configuration', async () => {
    const appShell = await waitForHttpOk(`${UI_URL}/`);
    const html = (await appShell.text()).toLowerCase();
    expect(html).toContain('<!doctype html>');
    expect(html).toContain('data-sveltekit-preload-data');

    const configResponse = await waitForHttpOk(`${UI_URL}/api/config`);
    const config = await configResponse.json() as BootstrapConfiguration;
    expect(config.mode).toEqual(expect.any(String));
    expect(config.version).toEqual(expect.any(String));
    expect(config.buildTimestamp).toEqual(expect.any(String));
    expect(config.oidcEnabled).toEqual(expect.any(Boolean));
  });
});
