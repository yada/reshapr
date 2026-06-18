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
import { GATEWAY_URL } from './setup.js';
import { E2E_ORG } from './fixtures.js';

export interface McpTool {
  name: string;
}

export interface McpResponse {
  jsonrpc: string;
  id: number;
  result?: any;
  error?: {
    code: number;
    message: string;
    data?: any;
  };
}

interface ToolExpectation {
  exact?: number;
  minimum?: number;
  include?: string[];
  exclude?: string[];
}

const MCP_PROTOCOL_VERSION = '2025-06-18';

function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}

export function mcpServiceName(name: string): string {
  return name.replaceAll(' ', '+');
}

export function mcpUrl(serviceName: string, serviceVersion: string): string {
  return `${GATEWAY_URL}/mcp/${E2E_ORG}/${mcpServiceName(serviceName)}/${serviceVersion}`;
}

export async function mcpRequest(url: string, method: string, params: any = {}, sessionId?: string): Promise<McpResponse> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };
  if (sessionId) {
    headers['MCP-Session-Id'] = sessionId;
    headers['MCP-Protocol-Version'] = MCP_PROTOCOL_VERSION;
  }

  const response = await fetch(url, {
    method: 'POST',
    headers,
    body: JSON.stringify({
      jsonrpc: '2.0',
      id: Date.now(),
      method,
      params,
    }),
  });

  const body = await response.text();
  if (!response.ok) {
    throw new Error(`MCP ${method} failed with HTTP ${response.status}: ${body}`);
  }
  return JSON.parse(body) as McpResponse;
}

export async function initializeMcp(url: string, capabilities: Record<string, any> = {}): Promise<{ body: McpResponse; sessionId?: string }> {
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      jsonrpc: '2.0',
      id: Date.now(),
      method: 'initialize',
      params: {
        protocolVersion: MCP_PROTOCOL_VERSION,
        capabilities,
        clientInfo: { name: 'reshapr-cli-e2e', version: '0.0.1' },
      },
    }),
  });

  const bodyText = await response.text();
  if (!response.ok) {
    throw new Error(`MCP initialize failed with HTTP ${response.status}: ${bodyText}`);
  }

  return {
    body: JSON.parse(bodyText) as McpResponse,
    sessionId: response.headers.get('mcp-session-id') ?? undefined,
  };
}

export async function listMcpTools(url: string): Promise<McpTool[]> {
  const body = await mcpRequest(url, 'tools/list', {});
  if (body.error) {
    throw new Error(`MCP tools/list returned error: ${JSON.stringify(body.error)}`);
  }
  const tools = body.result?.tools;
  if (!Array.isArray(tools)) {
    throw new Error(`MCP tools/list did not return result.tools[]: ${JSON.stringify(body)}`);
  }
  return tools as McpTool[];
}

export async function waitForMcpTools(url: string, expectation: ToolExpectation): Promise<McpTool[]> {
  const deadline = Date.now() + 60_000;
  let lastError: unknown;

  while (Date.now() < deadline) {
    try {
      const tools = await listMcpTools(url);
      assertToolExpectation(tools, expectation);
      return tools;
    } catch (error) {
      lastError = error;
      await sleep(2_000);
    }
  }

  throw lastError instanceof Error ? lastError : new Error('MCP tools did not satisfy expectation');
}

function assertToolExpectation(tools: McpTool[], expectation: ToolExpectation): void {
  const names = new Set(tools.map(tool => tool.name));
  if (expectation.exact !== undefined && tools.length !== expectation.exact) {
    throw new Error(`Expected exactly ${expectation.exact} MCP tools, got ${tools.length}`);
  }
  if (expectation.minimum !== undefined && tools.length < expectation.minimum) {
    throw new Error(`Expected at least ${expectation.minimum} MCP tools, got ${tools.length}`);
  }
  for (const name of expectation.include ?? []) {
    if (!names.has(name)) {
      throw new Error(`Expected MCP tool '${name}' to be present`);
    }
  }
  for (const name of expectation.exclude ?? []) {
    if (names.has(name)) {
      throw new Error(`Expected MCP tool '${name}' to be absent`);
    }
  }
}

export async function callMcpTool(url: string, name: string, args: Record<string, any>, sessionId?: string): Promise<McpResponse> {
  return mcpRequest(url, 'tools/call', { name, arguments: args }, sessionId);
}
