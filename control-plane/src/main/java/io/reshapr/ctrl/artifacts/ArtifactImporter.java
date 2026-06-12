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

import java.util.List;

/**
 * ArtifactImporter provides method definition for translating an artifact
 * specification file into Reshapr service definition
 * @author laurent
 */
public interface ArtifactImporter {

   void setServiceName(String serviceName);

   void setServiceVersion(String serviceVersion);

   List<Service> getServiceDefinitions() throws ArtifactImportException;

   List<Artifact> getArtifactDefinitions(Service service) throws ArtifactImportException;
}
