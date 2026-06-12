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
import io.reshapr.ctrl.model.Service;
import io.reshapr.ctrl.model.ServiceType;
import io.reshapr.util.ReferenceResolver;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test class for ProtobufImporter.
 * @author laurent
 */
class ProtobufImporterTest {

   @Test
   void testProtobufWithComplexRemoteDependenciesImport() {
      ProtobufImporter importer = null;
      try {
         importer = new ProtobufImporter("target/test-classes/io/reshapr/ctrl/artifacts/storage.proto",
               new ReferenceResolver(
                     "https://raw.githubusercontent.com/googleapis/googleapis/refs/heads/master/google/storage/v2/storage.proto",
                     null, true));
      } catch (IOException ioe) {
         fail("Exception should not be thrown");
      }

      // Check that basic service properties are there.
      List<Service> services = null;
      try {
         services = importer.getServiceDefinitions();
      } catch (ArtifactImportException e) {
         fail("Service definition import should not fail");
      }
      assertEquals(1, services.size());

      Service service = services.getFirst();
      assertEquals("google.storage.v2.Storage", service.name);
      assertEquals(ServiceType.GRPC, service.type);
      assertEquals("v2", service.version);

      // Check that resources have been parsed, correctly renamed, etc...
      List<Artifact> artifacts = null;
      try {
         artifacts = importer.getArtifactDefinitions(service);
      } catch (ArtifactImportException mrie) {
         fail("Resource definition import should not fail");
      }
      assertEquals(15, artifacts.size());
   }

   @Test
   void testProtobufWithComplexRemoteDependenciesImport2() {
      ProtobufImporter importer = null;
      try {
         importer = new ProtobufImporter("target/test-classes/io/reshapr/ctrl/artifacts/firestore.proto",
               new ReferenceResolver(
                     "https://raw.githubusercontent.com/googleapis/googleapis/refs/heads/master/google/firestore/v1/firestore.proto",
                     null, true));
      } catch (IOException ioe) {
         fail("Exception should not be thrown");
      }

      // Check that basic service properties are there.
      List<Service> services = null;
      try {
         services = importer.getServiceDefinitions();
      } catch (ArtifactImportException e) {
         fail("Service definition import should not fail");
      }
      assertEquals(1, services.size());

      Service service = services.getFirst();
      assertEquals("google.firestore.v1.Firestore", service.name);
      assertEquals(ServiceType.GRPC, service.type);
      assertEquals("v1", service.version);

      // Check that artifacts have been parsed, correctly renamed, etc...
      List<Artifact> artifacts = null;
      try {
         artifacts = importer.getArtifactDefinitions(service);
      } catch (ArtifactImportException mrie) {
         fail("Resource definition import should not fail");
      }
   }
}
