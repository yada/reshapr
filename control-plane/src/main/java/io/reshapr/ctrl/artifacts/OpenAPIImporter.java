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
package io.reshapr.ctrl.artifacts;

import io.reshapr.ctrl.model.Artifact;
import io.reshapr.ctrl.model.ArtifactType;
import io.reshapr.ctrl.model.Operation;
import io.reshapr.ctrl.model.Service;
import io.reshapr.ctrl.model.ServiceType;
import io.reshapr.util.ReferenceResolver;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * An implementation of ArtifactImporter that deals with OpenAPI v3.x.x specification file ; whether encoding into
 * JSON or YAML documents.
 * @author laurent
 */
public class OpenAPIImporter extends AbstractJsonArtifactImporter implements ArtifactImporter {

   private static final List<String> VALID_VERBS = Arrays.asList("get", "put", "post", "delete", "options", "head",
         "patch", "trace");

   protected String serviceName;
   protected String serviceVersion;

   /**
    * Build a new importer.
    * @param specificationFilePath The path to local OpenAPI spec file
    * @param referenceResolver     An optional resolver for references present into the OpenAPI file
    * @throws IOException if project file cannot be found or read.
    */
   public OpenAPIImporter(String specificationFilePath, ReferenceResolver referenceResolver) throws IOException {
      super(specificationFilePath, referenceResolver);
   }

   @Override
   public void setServiceName(String serviceName) {
      this.serviceName = serviceName;
   }

   @Override
   public void setServiceVersion(String serviceVersion) {
      this.serviceVersion = serviceVersion;
   }

   @Override
   public List<Service> getServiceDefinitions() throws ArtifactImportException {
      // Build a new service.
      Service service = new Service();

      service.name = serviceName != null ? serviceName
            : rootSpecification.path("info").path("title").asText();
      service.version = serviceVersion != null ? serviceVersion
            : rootSpecification.path("info").path("version").asText();
      service.type = ServiceType.REST;

      // Before extraction operations, we need to get and build external reference if we have a resolver.
      initializeReferencedArtifacts(service);

      // Then build its operations.
      service.operations = extractOperations();

      return List.of(service);
   }

   @Override
   public List<Artifact> getArtifactDefinitions(Service service) {
      List<Artifact> results = new ArrayList<>();

      // Build a suitable name.
      String name = service.name + "-" + service.version;
      if (Boolean.TRUE.equals(isYaml)) {
         name += ".yaml";
      } else {
         name += ".json";
      }

      // Build a brand-new resource just with spec content.
      Artifact resource = new Artifact();
      resource.name = name;
      resource.type = ArtifactType.OPEN_API_SPEC;
      results.add(resource);
      // Set the content of main OpenAPI that may have been updated with normalized dependencies with initializeReferencedResources().
      resource.content = rootSpecificationContent;

      // Add the external resources that were imported during service discovery.
      results.addAll(externalArtifacts);

      return results;
   }

   /**
    * Extract the list of operations from Specification.
    */
   private List<Operation> extractOperations() {
      List<Operation> results = new ArrayList<>();

      // Iterate on specification "paths" nodes.
      Set<Entry<String, JsonNode>> paths = rootSpecification.path("paths").properties();
      for (Entry<String, JsonNode> path : paths) {
         String pathName = path.getKey();
         JsonNode pathValue = followRefIfAny(path.getValue());

         // Iterate on specification path, "verbs" nodes.
         Set<Entry<String, JsonNode>> verbs = pathValue.properties();
         for (Entry<String, JsonNode> verb : verbs) {
            String verbName = verb.getKey();

            // Only deal with real verbs for now.
            if (VALID_VERBS.contains(verbName)) {
               String operationName = verbName.toUpperCase() + " " + pathName.trim();

               Operation operation = new Operation();
               operation.name = operationName;
               operation.method = verbName.toUpperCase();

               results.add(operation);
            }
         }
      }
      return results;
   }
}
