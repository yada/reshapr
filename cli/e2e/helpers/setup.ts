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
import { execa } from 'execa';
import * as path from 'node:path';

const COMPOSE_FILE = path.resolve(import.meta.dirname, '../../../install/docker-compose-all-in-one.yml');
const CONTROL_PLANE_URL = 'http://localhost:5555';
const GATEWAY_URL = 'http://localhost:7777';
const ADMIN_API_KEY = 'CzBuQ9B0i8qrUQe6WLiDLqR3gv4iCbxvjTJQP0z0CFGQbjgBHPZSusa9d1gZKwwjdoCsJ8ogRwRzc06GipJSjSDkFOy0BSOKvAa2EjU3As9I5UjgizTzxsJAVJIXtdo2xiXHhcry9KeJa0zRhDtGmm8WMujoXrlfj0ChlJKaHZiZsRthd4UHrWkKur9KySXpPFP21H4C0Cq6OgM1rJpvMZ7Jd2ZzeEcd5lKE4PlchHZBVEdu8jYzjQtU50fkOPoR';

export { CONTROL_PLANE_URL, GATEWAY_URL, ADMIN_API_KEY };

/**
 * Poll a URL until it returns HTTP 200, with retries.
 */
async function waitForReady(url: string, label: string, timeoutMs = 90_000, intervalMs = 2_000): Promise<void> {
  const deadline = Date.now() + timeoutMs;
  const healthUrl = `${url}/q/health/ready`;
  console.log(`⏳ Waiting for ${label} to be ready at ${healthUrl} ...`);

  while (Date.now() < deadline) {
    try {
      const res = await fetch(healthUrl);
      if (res.ok) {
        console.log(`✅ ${label} is ready.`);
        return;
      }
    } catch {
      // not ready yet
    }
    await new Promise(r => setTimeout(r, intervalMs));
  }
  throw new Error(`❌ ${label} did not become ready within ${timeoutMs / 1000}s`);
}

/**
 * Create a test user + organization via the admin API so the CLI can log in.
 */
async function provisionTestUser(): Promise<void> {
  console.log('👤 Creating test user and organization...');

  // The docker-compose already creates an "admin" user with password "password"
  // in the "reshapr" organization. We also create a dedicated e2e user.
  const serverUrl = CONTROL_PLANE_URL;

  // Create user
  await fetch(`${serverUrl}/api/admin/users`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'x-reshapr-api-key': ADMIN_API_KEY,
    },
    body: JSON.stringify({
      username: 'e2euser',
      email: 'e2e@reshapr.io',
      password: 'e2e-password',
      firstName: 'E2E',
      lastName: 'Tester',
    }),
  });

  // Create organization
  await fetch(`${serverUrl}/api/admin/users/e2euser/organization`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'x-reshapr-api-key': ADMIN_API_KEY,
    },
    body: JSON.stringify({
      name: 'e2eorg',
      description: 'Organization for E2E tests',
    }),
  });

  // Assign quotas
  await fetch(`${serverUrl}/api/admin/quotas/organization/e2eorg`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'x-reshapr-api-key': ADMIN_API_KEY,
    },
    body: JSON.stringify([
      { metric: 'gateway-group.count', enabled: true, limit: 3 },
      { metric: 'gateway.count', enabled: true, limit: 3 },
      { metric: 'exposition.count', enabled: true, limit: 10 },
    ]),
  });

  console.log('✅ Test user "e2euser" provisioned in org "e2eorg".');
}

export async function startInfrastructure(): Promise<void> {
  console.log('🚀 Starting infrastructure via Docker Compose...');
  await execa('docker', ['compose', '-f', COMPOSE_FILE, 'up', '-d', '--wait'], {
    stdio: 'inherit',
  });

  // Wait for both services to be healthy
  await waitForReady(CONTROL_PLANE_URL, 'Control Plane');
  await waitForReady(GATEWAY_URL, 'Gateway');

  // Provision the test user
  await provisionTestUser();
}

export async function stopInfrastructure(): Promise<void> {
  console.log('🛑 Stopping infrastructure...');
  await execa('docker', ['compose', '-f', COMPOSE_FILE, 'down', '-v'], {
    stdio: 'inherit',
  });
  console.log('✅ Infrastructure stopped.');
}
