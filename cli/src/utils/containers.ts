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
import { spawn } from 'node:child_process';

import { Logger } from "./logger.js";

export function runDockerCompose(args: string[], composeFile: string): Promise<number> {
  return new Promise((resolve, reject) => {
    const proc = spawn('docker', ['compose', '-f', composeFile, ...args], {
      stdio: 'inherit',
      shell: false,
    });
    proc.on('close', (code) => resolve(code ?? 1));
    proc.on('error', (err) => {
      Logger.error(`Failed to execute docker compose: ${err.message}`);
      reject(err);
    });
  });
}