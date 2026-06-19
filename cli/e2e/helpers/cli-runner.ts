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
import { execaNode } from 'execa';
import * as path from 'node:path';
import * as fs from 'node:fs';
import * as os from 'node:os';

const CLI_ENTRY = path.resolve(import.meta.dirname, '../../dist/cli.js');

// Create a temporary HOME directory so the CLI config (~/.reshapr/config)
// is isolated from the developer's real environment.
let _tempHome: string | undefined;

export function getTempHome(): string {
  if (!_tempHome) {
    _tempHome = fs.mkdtempSync(path.join(os.tmpdir(), 'reshapr-e2e-'));
  }
  return _tempHome;
}

export function cleanTempHome(): void {
  if (_tempHome) {
    fs.rmSync(_tempHome, { recursive: true, force: true });
    _tempHome = undefined;
  }
}

export interface CliResult {
  stdout: string;
  stderr: string;
  exitCode: number;
}

/**
 * Run the reshapr CLI with the given arguments.
 * stdout and stderr are captured; the process never throws on non-zero exit.
 */
export async function runCli(...args: string[]): Promise<CliResult> {
  const result = await execaNode(CLI_ENTRY, args, {
    env: {
      ...process.env,
      HOME: getTempHome(),
      // Avoid interactive prompts in CI
      CI: 'true',
    },
    reject: false,  // don't throw on non-zero exit
    timeout: 60_000,
  });

  return {
    stdout: result.stdout,
    stderr: result.stderr,
    exitCode: result.exitCode ?? 1,
  };
}

/**
 * Run the CLI and expect success (exit code 0).
 * Throws a descriptive error when the CLI fails.
 */
export async function runCliExpectSuccess(...args: string[]): Promise<CliResult> {
  const result = await runCli(...args);
  if (result.exitCode !== 0) {
    throw new Error(
      `CLI exited with code ${result.exitCode}\n` +
      `args: ${args.join(' ')}\n` +
      `stdout: ${result.stdout}\n` +
      `stderr: ${result.stderr}`
    );
  }
  return result;
}

export async function runCliJson<T = unknown>(...args: string[]): Promise<T> {
  const jsonArgs = [...args, '-o', 'json'];
  const result = await runCliExpectSuccess(...jsonArgs);
  const output = result.stdout.trim();
  if (!output) {
    throw new Error(`CLI command did not return JSON\nargs: ${jsonArgs.join(' ')}`);
  }

  try {
    return JSON.parse(output) as T;
  } catch (error) {
    throw new Error(
      `CLI command returned invalid JSON\n` +
      `args: ${jsonArgs.join(' ')}\n` +
      `stdout: ${result.stdout}\n` +
      `error: ${(error as Error).message}`
    );
  }
}

export async function login(): Promise<void> {
  await runCliExpectSuccess(
    'login',
    '-u', 'e2euser',
    '-p', 'e2e-password',
    '-s', 'http://localhost:5555',
    '-k',
  );
}
