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
import { runCli, runCliExpectSuccess } from './cli-runner.js';

interface ServiceRef {
  id: string;
  name: string;
  version: string;
}

interface SecretRef {
  id: string;
  name: string;
}

async function listServices(): Promise<ServiceRef[]> {
  const result = await runCli('service', 'list', '-o', 'json');
  if (result.exitCode !== 0 || !result.stdout.trim()) {
    return [];
  }
  return JSON.parse(result.stdout) as ServiceRef[];
}

async function listSecrets(): Promise<SecretRef[]> {
  const result = await runCli('secret', 'list', '-o', 'json');
  if (result.exitCode !== 0 || !result.stdout.trim()) {
    return [];
  }
  return JSON.parse(result.stdout) as SecretRef[];
}

export async function deleteServiceIfPresent(serviceId: string | undefined): Promise<void> {
  if (!serviceId) {
    return;
  }
  await runCli('service', 'delete', serviceId, '-f');
}

export async function deleteSecretIfPresent(secretId: string | undefined): Promise<void> {
  if (!secretId) {
    return;
  }
  await runCli('secret', 'delete', secretId);
}

export async function deleteServicesByNameVersion(name: string, version: string): Promise<void> {
  const services = await listServices();
  for (const service of services) {
    if (service.name === name && service.version === version) {
      await runCliExpectSuccess('service', 'delete', service.id, '-f');
    }
  }
}

export async function deleteSecretsByName(name: string): Promise<void> {
  const secrets = await listSecrets();
  for (const secret of secrets) {
    if (secret.name === name) {
      await runCliExpectSuccess('secret', 'delete', secret.id);
    }
  }
}
