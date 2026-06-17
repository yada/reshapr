/*
 * Copyright The Reshapr Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import EditorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker';
import YamlWorker from './yaml.worker.js?worker';

import type * as Monaco from 'monaco-editor';

let monacoPromise: Promise<typeof Monaco> | null = null;

function configureWorkers(): void {
	if (globalThis.MonacoEnvironment) return;

	globalThis.MonacoEnvironment = {
		getWorker(_moduleId: string, label: string) {
			switch (label) {
				case 'editorWorkerService':
					return new EditorWorker();
				case 'yaml':
					return new YamlWorker();
				default:
					throw new Error(`Unknown Monaco worker label: ${label}`);
			}
		}
	};
}

/** Client-only Monaco + YAML language registration (schemas wired in release 3). */
export async function ensureMonacoYaml(): Promise<typeof Monaco> {
	if (monacoPromise) return monacoPromise;

	monacoPromise = (async () => {
		configureWorkers();
		await import('monaco-editor/min/vs/editor/editor.main.css');
		const monaco = await import('monaco-editor');
		const { configureMonacoYaml } = await import('monaco-yaml');

		configureMonacoYaml(monaco, {
			enableSchemaRequest: true,
			schemas: []
		});

		return monaco;
	})();

	return monacoPromise;
}
