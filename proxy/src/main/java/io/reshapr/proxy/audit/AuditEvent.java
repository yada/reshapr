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
package io.reshapr.proxy.audit;

import jakarta.annotation.Nullable;

/**
 * Immutable record representing an MCP audit event. Serves as the contract between
 * instrumentation points (controllers) and the {@link AuditLogger}.
 *
 * @param method The MCP method invoked (e.g. "tools/call", "resources/read").
 * @param targetName The name of the tool or resource invoked, if applicable.
 * @param outcome The outcome of the call: "success" or "failure".
 * @param errorCode The JSONRPC error code, if the call failed.
 * @param durationMs Duration of the call in milliseconds.
 * @param serviceName The service name.
 * @param serviceVersion The service version.
 * @param organizationId The organization ID.
 * @param requestId The JSONRPC request ID.
 * @param sessionId The MCP session ID, if available.
 * @param sourceIp The remote IP address of the caller.
 * @param userId The authenticated user ID (JWT subject), if available.
 * @author laurent
 */
public record AuditEvent(
      String method,
      @Nullable String targetName,
      String outcome,
      @Nullable Integer errorCode,
      long durationMs,
      String serviceName,
      String serviceVersion,
      String organizationId,
      @Nullable Object requestId,
      @Nullable String sessionId,
      @Nullable String sourceIp,
      @Nullable String userId,
      long responseSize,
      @Nullable String traceId
) {
   /** Convenience constants for outcome values. */
   public static final String OUTCOME_SUCCESS = "success";
   public static final String OUTCOME_FAILURE = "failure";
}
