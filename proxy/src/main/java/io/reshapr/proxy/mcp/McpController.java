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
package io.reshapr.proxy.mcp;

import io.reshapr.proxy.mcp.converters.GraphQLMcpToolConverter;
import io.reshapr.proxy.mcp.converters.GrpcMcpToolConverter;
import io.reshapr.proxy.mcp.converters.McpToolConverter;
import io.reshapr.proxy.mcp.converters.OpenAPIMcpToolConverter;
import io.reshapr.proxy.mcp.converters.ReshaprCustomToolsMcpToolConverter;
import io.reshapr.proxy.mcp.state.ElicitationStore;
import io.reshapr.proxy.context.SessionInfo;
import io.reshapr.proxy.mcp.state.SessionStore;
import io.reshapr.proxy.proxy.GrpcProxyService;
import io.reshapr.proxy.proxy.ProxyService;
import io.reshapr.proxy.context.MethodHandlingInfo;
import io.reshapr.proxy.context.MethodHandlingContext;
import io.reshapr.proxy.registry.ArtifactEntryType;
import io.reshapr.proxy.registry.ConfigurationEntry;
import io.reshapr.proxy.registry.GatewayRegistry;
import io.reshapr.proxy.registry.OperationEntry;
import io.reshapr.proxy.registry.SecretEntry;
import io.reshapr.proxy.registry.ServiceEntry;
import io.reshapr.proxy.security.SecureEndpoint;
import io.reshapr.proxy.util.WebUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.instrumentation.annotations.AddingSpanAttributes;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.core.http.HttpServerRequest;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@RunOnVirtualThread
@Path("/mcp")
public class McpController {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   private final GatewayRegistry gatewayRegistry;
   private final SessionStore sessionStore;
   private final ElicitationStore elicitationStore;
   private final WorkCache workCache;
   private final ProxyService proxyService;
   private final GrpcProxyService grpcProxyService;

   private final ObjectMapper mapper = new ObjectMapper();

   @ConfigProperty(name = "reshapr.gateway.fqdns", defaultValue = "[localhost:7777]")
   List<String> fqdns;

   /**
    * Build a McpController with required dependencies.
    * @param gatewayRegistry The registry to access services and configurations.
    * @param sessionStore The store for managing MCP sessions.
    * @param workCache The work cache for temporary data storage.
    * @param proxyService   The proxy service for handling HTTP proxying.
    * @param grpcProxyService The gRPC proxy service for handling gRPC proxying.
    */
   public McpController(GatewayRegistry gatewayRegistry, SessionStore sessionStore, ElicitationStore elicitationStore,
                        WorkCache workCache, ProxyService proxyService, GrpcProxyService grpcProxyService) {
      this.gatewayRegistry = gatewayRegistry;
      this.sessionStore = sessionStore;
      this.elicitationStore = elicitationStore;
      this.workCache = workCache;
      this.proxyService = proxyService;
      this.grpcProxyService = grpcProxyService;
   }

   @POST
   @Path("/{serviceId}")
   @Produces(MediaType.APPLICATION_JSON)
   @SecureEndpoint
   @AddingSpanAttributes
   public Response handleHttpStreamable(@SpanAttribute("serviceId") @PathParam("serviceId") String serviceId,
                                        McpSchema.JSONRPCRequest request, HttpHeaders headers, HttpServerRequest serverRequest) {

      ServiceEntry serviceEntry = gatewayRegistry.getService(serviceId);
      if (serviceEntry == null) {
         String errorMsg = String.format("Service with id '%s' not found", serviceId);
         logger.warn(errorMsg);
         return Response.status(Response.Status.NOT_FOUND).entity(errorMsg).build();
      }

      return handleMcpRequest(serviceEntry, request, headers, serverRequest);
   }

   @POST
   @Path("/{organizationId}/{service}/{version}")
   @Produces(MediaType.APPLICATION_JSON)
   @SecureEndpoint
   @AddingSpanAttributes
   public Response handleHttpStreamable(@SpanAttribute("organizationId") @PathParam("organizationId") String organizationId,
                                        @SpanAttribute("service") @PathParam("service") String service,
                                        @SpanAttribute("version") @PathParam("version") String version,
                                        McpSchema.JSONRPCRequest request, HttpHeaders headers, HttpServerRequest serverRequest) {

      // If serviceName was encoded with '+' instead of '%20', remove them.
      if (service.contains("+")) {
         service = service.replace('+', ' ');
      }

      ServiceEntry serviceEntry = gatewayRegistry.getService(organizationId, service, version);
      if (serviceEntry == null) {
         String errorMsg = String.format("Service '%s', version: '%s' in organization: '%s' not found", service, version, organizationId);
         logger.warn(errorMsg);
         return Response.status(Response.Status.NOT_FOUND).entity(errorMsg).build();
      }

      return handleMcpRequest(serviceEntry, request, headers, serverRequest);
   }

   private Response handleMcpRequest(ServiceEntry service, McpSchema.JSONRPCRequest request, HttpHeaders headers, HttpServerRequest serverRequest) {
      logger.debugf("Handling a Mcp Http call on service: %s", service.id());
      logger.debugf("Request body: %s", request);
      logger.debugf("Request headers: %s", headers.getRequestHeaders());

      AtomicReference<McpHandlerResult> resultRef = new AtomicReference<>();
      try {
         // Scope the call with call + session info for those who need it.
         MethodHandlingInfo handlingInfo = new MethodHandlingInfo(
               serverRequest.remoteAddress().host(),
               getSessionInfo(headers));
         ScopedValue.where(MethodHandlingContext.METHOD_HANDLING_INFO, handlingInfo).run(() -> {
            resultRef.set(handleMcpRequest(service, request, headers));
         });

         // Compose a Response based on result.
         McpHandlerResult result = resultRef.get();
         Response.ResponseBuilder responseBuilder = Response.ok(result.message());
         if (result.headers() != null) {
            result.headers().forEach((key, value) -> value.forEach(
                  headerValue -> responseBuilder.header(key, headerValue)
            ));
         }

         // Now add the mandatory MCP headers bound to session.
         if (getSessionInfo(headers) != null) {
            SessionInfo sessionInfo = getSessionInfo(headers);
            if (sessionInfo != null) {
               logger.debugf("Adding MCP session headers for session id: %s", sessionInfo.getId());
               responseBuilder
                     .header(McpSchema.HEADER_SESSION_ID, sessionInfo.getId())
                     .header(McpSchema.HEADER_PROTOCOL_VERSION, sessionInfo.getProtocolVersion());
            }
         }

         return responseBuilder.build();
      } catch (McpError e) {
         return Response.status(Response.Status.BAD_REQUEST).entity(e).build();
      }
   }

   @Nullable
   private SessionInfo getSessionInfo(HttpHeaders headers) {
      if (headers.getRequestHeader(McpSchema.HEADER_SESSION_ID) != null &&
            !headers.getRequestHeader(McpSchema.HEADER_SESSION_ID).isEmpty()) {
         String sessionId = headers.getRequestHeader(McpSchema.HEADER_SESSION_ID).getFirst();
         return sessionStore.getSessionInfo(sessionId);
      }
      return null;
   }

   /**
    * Handle the MCP request and return a JSONRPCResponse.
    * @param service The service entry for which the request is made.
    * @param request The JSONRPCRequest to handle.
    * @param headers The HTTP headers associated with the request.
    * @return A JSONRPCMessage representing the result of the request handling.
    */
   private McpHandlerResult handleMcpRequest(ServiceEntry service, McpSchema.JSONRPCRequest request, HttpHeaders headers) {
      McpHandlerResult result = null;
      switch (request.method()) {
         case McpSchema.METHOD_INITIALIZE ->
            result = handleInitializeRequest(request, service);

         case McpSchema.METHOD_PROMPTS_LIST ->
            result = handlePromptListRequest(request, service);

         case McpSchema.METHOD_PROMPTS_GET ->
            result = handlePromptGetRequest(request, service);

         case McpSchema.METHOD_RESOURCES_LIST ->
            result = handleResourceListRequest(request, service);

         case McpSchema.METHOD_RESOURCES_TEMPLATES_LIST ->
            result = handleResourceTemplateListRequest(request, service);

         case McpSchema.METHOD_RESOURCES_READ ->
            result = handleResourceReadRequest(request, service);

         case McpSchema.METHOD_TOOLS_LIST ->
            result = handleToolsListRequest(request, service);

         case McpSchema.METHOD_TOOLS_CALL ->
            result = handleToolsCallRequest(request, headers.getRequestHeaders(), service);
      }

      if (result == null) {
         // No result means method not found JSONRPCError.
         logger.warnf("Unsupported MCP method: %s", request.method());
         return new McpHandlerResult(new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.id(), null,
               new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.METHOD_NOT_FOUND,
                     "Unsupported method: " + request.method(), null)), null);
      }
      return result;
   }

   private record McpHandlerResult(
         McpSchema.JSONRPCMessage message,
         @Nullable Map<String, List<String>> headers
   ) {
      public boolean isJSONRPCRequest() {
         return message instanceof McpSchema.JSONRPCRequest;
      }
      public boolean isJSONRPCResponse() {
         return message instanceof McpSchema.JSONRPCResponse;
      }
   }

   /** Handle the MCP initialize request. */
   private McpHandlerResult handleInitializeRequest(McpSchema.JSONRPCRequest request, ServiceEntry service) {
      McpSchema.InitializeRequest initializeRequest = mapper.convertValue(request.params(),
            new TypeReference<McpSchema.InitializeRequest>() {
            });

      if (McpSchema.SUPPORTED_PROTOCOL_VERSIONS.contains(initializeRequest.protocolVersion())) {
         McpSchema.ClientCapabilities clientCapabilities = initializeRequest.capabilities();
         McpSchema.Implementation clientInfo = initializeRequest.clientInfo();

         McpSchema.ServerCapabilities serverCapabilities = new McpSchema.ServerCapabilities(null, null,
               new McpSchema.ServerCapabilities.PromptCapabilities(false),
               new McpSchema.ServerCapabilities.ResourceCapabilities(false, false),
               new McpSchema.ServerCapabilities.ToolCapabilities(false));

         McpSchema.JSONRPCResponse response = buildJSONRPCResponse(request,
               new McpSchema.InitializeResult(initializeRequest.protocolVersion(), serverCapabilities,
                     new McpSchema.Implementation(service.name() + " MCP server", service.version()), null));

         // Initialize session and return session ID in headers.
         String sessionId = sessionStore.initializeSession(service.id(), initializeRequest.protocolVersion());
         Map<String, List<String>> responseHeaders = Map.of(McpSchema.HEADER_SESSION_ID,
               List.of(sessionId));

         return new McpHandlerResult(response, responseHeaders);
      }
      // Return error for unsupported protocol version.
      return new McpHandlerResult(buildJSONRPCError(request, McpSchema.ErrorCodes.INVALID_PARAMS,
            "Unsupported protocol version: " + initializeRequest.protocolVersion(),
            Map.of("supported", McpSchema.SUPPORTED_PROTOCOL_VERSIONS, "requested", initializeRequest.protocolVersion())), null);
   }

   /** Handle the MCP prompt/list request. */
   private McpHandlerResult handlePromptListRequest(McpSchema.JSONRPCRequest request, ServiceEntry service) {
      // Build a MCP Prompt Builder based on available elements in registry.
      McpPromptBuilder builder = buildMcpPromptBuilder(service);

      return toMcpHandlerResult(request, new McpSchema.ListPromptsResult(builder.listPrompts(), null));
   }

   /** Handle the MCP prompt/get request. */
   private McpHandlerResult handlePromptGetRequest(McpSchema.JSONRPCRequest request, ServiceEntry service) {
      McpSchema.SimpleRequest promptGetRequest = mapper.convertValue(request.params(),
            new TypeReference<McpSchema.SimpleRequest>() {
            });

      // Build a MCP Prompt Builder based on available elements in registry.
      McpPromptBuilder builder = buildMcpPromptBuilder(service);

      McpSchema.PromptMessage prompt = builder.getPrompt(promptGetRequest);

      return toMcpHandlerResult(request, new McpSchema.GetPromptResult(null, List.of(prompt)));
   }

   /** Handle the MCP resource/list request. */
   private McpHandlerResult handleResourceListRequest(McpSchema.JSONRPCRequest request, ServiceEntry service) {
      // Build a MCP Resource Builder based on available elements in registry.
      McpResourceBuilder builder = buildMcpResourceBuilder(service);

      return toMcpHandlerResult(request, new McpSchema.ListResourcesResult(builder.listResources(), null));
   }

   /** Handle the MCP resource/templates/list request. */
   private McpHandlerResult handleResourceTemplateListRequest(McpSchema.JSONRPCRequest request, ServiceEntry service) {
      // Build a MCP Resource Builder based on available elements in registry.
      McpResourceBuilder builder = buildMcpResourceBuilder(service);

      return toMcpHandlerResult(request, new McpSchema.ListResourceTemplatesResult(builder.listResourceTemplates(), null));
   }

   /** Handle the MCP resource/read request. */
   private McpHandlerResult handleResourceReadRequest(McpSchema.JSONRPCRequest request, ServiceEntry service) {
      McpSchema.ReadResourceRequest resourceReadRequest = mapper.convertValue(request.params(),
            new TypeReference<McpSchema.ReadResourceRequest>() {
            });

      // Get configuration plan for service.
      ConfigurationEntry configuration = gatewayRegistry.getConfiguration(service);

      // Build a MCP Resource Builder based on available elements in registry.
      McpResourceBuilder builder = buildMcpResourceBuilder(service);

      return toMcpHandlerResult(request, new McpSchema.ReadResourceResult(builder.readResource(resourceReadRequest, configuration)));
   }

   /** Handle the MCP tools/list request. */
   private McpHandlerResult handleToolsListRequest(McpSchema.JSONRPCRequest request, ServiceEntry service) {
      // Get configuration plan for service.
      ConfigurationEntry configuration = gatewayRegistry.getConfiguration(service);

      // Build converter based on service type.
      McpToolConverter converter = buildMcpToolConverter(service);

      List<McpSchema.Tool> tools = converter.getAvailableOperations(service).stream()
            .filter(operation -> isExposedOperation(configuration, operation))
            .map(operation -> new McpSchema.Tool(converter.getToolName(operation),
                  converter.getToolDescription(operation), converter.getInputSchema(operation),
                  converter.getToolMetadata(gatewayRegistry, service, operation)))
            .toList();

      return toMcpHandlerResult(request, new McpSchema.ListToolsResult(tools, null));
   }

   /** Handle the MCP tools/call request. */
   private McpHandlerResult handleToolsCallRequest(McpSchema.JSONRPCRequest request, Map<String, List<String>> headers,
         ServiceEntry service) {
      McpSchema.SimpleRequest toolRequest = mapper.convertValue(request.params(),
            new TypeReference<McpSchema.SimpleRequest>() {
            });

      ConfigurationEntry configuration = gatewayRegistry.getConfiguration(service);

      SecretEntry secret = configuration.backendSecret();
      if (secret != null && secret.useElicitation()) {
         logger.debugf("Checking elicitation secret value for secret '%s'", secret.name());

         SessionInfo sessionInfo = MethodHandlingContext.getSessionInfo();
         if (sessionInfo == null) {
            // Missing session info error as per the spec.
            logger.warn("Session information is missing for elicitation secret handling");
            return toMcpHandlerResult(request, McpSchema.ErrorCodes.INVALID_REQUEST,
                  "Session information is missing for elicitation secret handling", null);
         }

         logger.debugf("Session info is '%s'", sessionInfo);
         logger.debugf("Session secret value: %s", sessionInfo.getSecretValue(secret));

         if (sessionInfo.getSecretValue(secret) == null) {
            // Missing elicitation secret value error as per the spec.
            logger.debugf("Secret value for secret '%s' is missing, initializing elicitation", secret.name());
            String elicitationId = elicitationStore.initializeElicitation(sessionInfo.getId(), service.organizationId(),
                  configuration.backendEndpoint(), secret);

            // Adapt elicitation endpoint based on type.
            String elicitationPath = secret.oauth2ClientConfiguration() != null ? "/connect" : "/form";
            String elicitationUrl = WebUtils.getHTTPScheme(fqdns.getFirst()) + fqdns.getFirst() + "/elicitation"
                  + elicitationPath + "?elicitationId=" + elicitationId;

            logger.debugf("Elicitation URL is '%s'", elicitationUrl);
            McpSchema.URLElicitation elicitation = new McpSchema.URLElicitation(elicitationId, elicitationUrl,
                  "Please provide backend secret information by visiting the above URL.");
            return toMcpHandlerResult(request, McpSchema.buildURLElicitationRequiredError(List.of(elicitation)));
         }
      }

      // Build converter based on service type.
      McpToolConverter converter = buildMcpToolConverter(service);

      OperationEntry callOperation = converter.getAvailableOperations(service).stream()
            .filter(operation -> isExposedOperation(configuration, operation))
            .filter(operation -> toolRequest.name().equals(converter.getToolName(operation)))
            .findFirst().orElse(null);
      if (callOperation == null) {
         // Unknown tool error as per the spec.
         return toMcpHandlerResult(request, McpSchema.ErrorCodes.INVALID_PARAMS , "Unknown tool: " + toolRequest.name(),
               null);
      }

      // We copy headers before calling because original map is immutable.
      var response = converter.getCallResponse(callOperation, configuration, toolRequest, new HashMap<>(headers));

      return toMcpHandlerResult(request, new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(response.content())),
            response.isFault()));
   }

   private static McpHandlerResult toMcpHandlerResult(McpSchema.JSONRPCRequest request, Object result) {
      return new McpHandlerResult(new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.id(), result, null),
            null);
   }

   private static McpHandlerResult toMcpHandlerResult(McpSchema.JSONRPCRequest request, int code, String message, Object data) {
      return new McpHandlerResult(
            buildJSONRPCError(request, code, message, data),
            null);
   }

   private static McpHandlerResult toMcpHandlerResult(McpSchema.JSONRPCRequest request, McpSchema.JSONRPCResponse.JSONRPCError error) {
      return new McpHandlerResult(
            new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.id(), null, error),
            null);
   }

   private static McpSchema.JSONRPCResponse buildJSONRPCResponse(McpSchema.JSONRPCRequest request, Object result) {
      return new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.id(), result, null);
   }

   private static McpSchema.JSONRPCResponse buildJSONRPCError(McpSchema.JSONRPCRequest request, int code, String message, Object data) {
      return new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.id(), null,
         new McpSchema.JSONRPCResponse.JSONRPCError(code, message, data));
   }

   private McpPromptBuilder buildMcpPromptBuilder(ServiceEntry service) {
      return new ReshaprPromptsMcpPromptBuilder(service,
            gatewayRegistry.getAttachedArtifacts(service), workCache, mapper);
   }

   private McpResourceBuilder buildMcpResourceBuilder(ServiceEntry service) {
      return new ReshaprResourcesMcpResourceBuilder(service,
            gatewayRegistry.getAttachedArtifacts(service), workCache, mapper, proxyService);
   }

   private McpToolConverter buildMcpToolConverter(ServiceEntry service) {
      McpToolConverter converter = null;

      switch (service.type()) {
         case "GRAPHQL" -> converter = new GraphQLMcpToolConverter(service, gatewayRegistry.getMainArtifact(service),
               workCache, mapper, proxyService);
         case "GRPC" -> converter = new GrpcMcpToolConverter(service, gatewayRegistry.getMainArtifact(service),
               workCache, mapper, grpcProxyService);
         default -> converter = new OpenAPIMcpToolConverter(service, gatewayRegistry.getMainArtifact(service),
               gatewayRegistry.getAttachedArtifacts(service), workCache, mapper, proxyService);
      }

      // If we have Custom Tools artifacts attached, wrap converter.
      if (gatewayRegistry.getAttachedArtifacts(service) != null && gatewayRegistry.getAttachedArtifacts(service).stream()
            .anyMatch(artifactEntry -> ArtifactEntryType.RESHAPR_CUSTOM_TOOLS.equals(artifactEntry.type()))) {
         converter = new ReshaprCustomToolsMcpToolConverter(service, gatewayRegistry.getAttachedArtifacts(service),
               workCache, converter);
      }
      return converter;
   }

   private static boolean isExposedOperation(ConfigurationEntry configuration, OperationEntry operation) {
      if (!configuration.includedOperations().isEmpty()) {
         return configuration.includedOperations().contains(operation.name());
      }
      if (!configuration.excludedOperations().isEmpty()) {
         return !configuration.excludedOperations().contains(operation.name());
      }
      return true; // No exclusions or inclusions, so all operations are exposed by default.
   }
}
