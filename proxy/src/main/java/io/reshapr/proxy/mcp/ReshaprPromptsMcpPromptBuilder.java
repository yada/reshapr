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

import io.reshapr.json.ObjectMapperFactory;
import io.reshapr.proxy.registry.ArtifactEntry;
import io.reshapr.proxy.registry.ArtifactEntryType;
import io.reshapr.proxy.registry.ServiceEntry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * An implementation of McpPromptBuilder that builds prompts from Reshapr Prompts artifacts.
 * @author laurent
 */
class ReshaprPromptsMcpPromptBuilder implements McpPromptBuilder {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   private static final String CACHE_KEYS_PREFIX = "rpmcppb-";

   private static final ObjectMapper YAML_MAPPER = ObjectMapperFactory.getYamlObjectMapper();
   private static final String ARGUMENT_START_MARKER = "${";

   private final ServiceEntry service;
   private final List<ArtifactEntry> attachedArtifacts;
   private final WorkCache workCache;
   private final ObjectMapper mapper;
   
   public ReshaprPromptsMcpPromptBuilder(ServiceEntry service, List<ArtifactEntry> attachedArtifacts,
                                         WorkCache workCache, ObjectMapper mapper) {
      this.service = service;
      this.attachedArtifacts = attachedArtifacts;
      this.workCache = workCache;
      this.mapper = mapper;
   }

   @Override
   public List<McpSchema.Prompt> listPrompts() {
      // First, check cache to see if we have already loaded prompts.
      JsonNode promptsNode = getPromptsNode();
      if (promptsNode == null) {
         logger.debugf("No Prompts artifact found for service '%s'", service.id());
         return Collections.emptyList();
      }

      // Convert to list of Prompt objects.
      List<McpSchema.Prompt> prompts = new ArrayList<>();
      Iterator<String> namesIterator = promptsNode.fieldNames();
      while (namesIterator.hasNext()) {
         String name = namesIterator.next();

         JsonNode promptNode = promptsNode.get(name);
         String title = promptNode.has("title") ? promptNode.get("title").asText() : null;
         String description = promptNode.has("description") ? promptNode.get("description").asText() : null;
         List<McpSchema.PromptArgument> arguments = promptNode.has("arguments") ?
               mapper.convertValue(promptNode.get("arguments"),
                     new TypeReference<List<McpSchema.PromptArgument>>() {}) : Collections.emptyList();

         prompts.add(new McpSchema.Prompt(name, title, description, arguments));
      }
      return prompts;
   }

   @Override
   public McpSchema.PromptMessage getPrompt(McpSchema.SimpleRequest request) {
      // First, check cache to see if we have already loaded prompts.
      JsonNode promptsNode = getPromptsNode();
      if (promptsNode == null) {
         logger.debugf("No Prompts artifact found for service '%s'", service.id());
         return new McpSchema.PromptMessage(McpSchema.Role.ASSISTANT,
               new McpSchema.TextContent("No prompt messages found."));
      }

      // Find the requested prompt.
      JsonNode promptNode = promptsNode.get(request.name());
      if (promptNode == null) {
         logger.debugf("No prompt named '%s' found for service '%s'", request.name(), service.id());
         return new McpSchema.PromptMessage(McpSchema.Role.ASSISTANT,
               new McpSchema.TextContent("No prompt named '" + request.name() + "' found."));
      }

      // Build the prompt message content.
      String result = promptNode.path("result").asText();
      if (result.contains(ARGUMENT_START_MARKER) && promptNode.has("arguments")) {
         for (Map.Entry<String, Object> argument : request.arguments().entrySet()) {
            String name = argument.getKey();
            String value = argument.getValue().toString();

            if (value != null) {
               result = result.replace(ARGUMENT_START_MARKER + name + "}", value);
            }
         }
      }
      return new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent(result));
   }

   /** Get the root JsonNode containing prompts definitions, using cache when possible. */
   private @Nullable JsonNode getPromptsNode() {
      String major = String.valueOf(service.hashCode());
      if (workCache.get(major, CACHE_KEYS_PREFIX) instanceof JsonNode promptsNode) {
         logger.tracef("Got a cached value of Prompts JsonNode for service '%s'", service.id());
         return promptsNode;
      }

      // Avoid errors if no attached artifacts.
      if (attachedArtifacts == null || attachedArtifacts.isEmpty()) {
         return null;
      }

      // Check the existence of a Reshapr Prompts artifact.
      Optional<ArtifactEntry> promptsArtifact = attachedArtifacts.stream()
            .filter(artifactEntry -> ArtifactEntryType.RESHAPR_PROMPTS.equals(artifactEntry.type()))
            .findFirst();
      if (promptsArtifact.isEmpty()) {
         return null;
      }

      // Compute new value to cache.
      logger.debugf("Need to build Prompts for service '%s'", service.id());
      try {
         JsonNode artifactNode = YAML_MAPPER.readTree(promptsArtifact.get().content());
         JsonNode promptsNode = artifactNode.get("prompts");

         workCache.set(major, CACHE_KEYS_PREFIX, promptsNode);
         return promptsNode;
      } catch (Exception e) {
         logger.errorf(e, "Cannot read Reshapr Prompts artifact for service '%s'", service.id());
         return null;
      }
   }
}
