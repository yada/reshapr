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
import * as path from 'node:path';

export const E2E_ORG = 'e2eorg';

export const OPEN_METEO_SPEC = path.resolve(import.meta.dirname, '../../../dev/open-meteo-openapi.yml');
export const OPEN_METEO_SERVICE_NAME = 'Open-Meteo APIs';
export const OPEN_METEO_SERVICE_VERSION = '1.0';
export const OPEN_METEO_BACKEND = 'https://api.open-meteo.com';
export const OPEN_METEO_EXPECTED_TOOLS = 1;
export const OPEN_METEO_TOOL_NAME = 'get_v1_forecast';

export const GITHUB_GRAPHQL_SPEC = path.resolve(import.meta.dirname, '../../../dev/github-api.graphql');
export const GITHUB_CUSTOM_TOOLS = path.resolve(import.meta.dirname, '../../../dev/github-api-custom-tools.yaml');
export const GITHUB_GRAPHQL_SERVICE_NAME = 'GitHub GraphQL';
export const GITHUB_GRAPHQL_SERVICE_VERSION = '20250917';
export const GITHUB_GRAPHQL_BACKEND = 'https://api.github.com/graphql';
export const GITHUB_FILTERED_EXPECTED_TOOLS = 1;
export const GITHUB_CUSTOM_EXPECTED_TOOLS = 1;
export const GITHUB_RAW_USER_TOOL = 'user';
export const GITHUB_CUSTOM_USER_TOOL = 'get_user_with_latest_followers';
export const GITHUB_ELICITATION_SECRET_NAME = 'github-e2e-elicitation';
export const GITHUB_ELICITATION_LOGIN = 'octocat';

export function expectedGithubFullToolCount(): number | undefined {
  const value = process.env.RESHAPR_GITHUB_FULL_EXPECTED_TOOLS;
  return value ? Number(value) : undefined;
}

export function githubFullMinimumToolCount(): number {
  return Number(process.env.RESHAPR_GITHUB_FULL_MIN_TOOLS ?? '50');
}
