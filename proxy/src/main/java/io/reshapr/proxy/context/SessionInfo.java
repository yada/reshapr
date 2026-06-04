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
package io.reshapr.proxy.context;

import io.reshapr.proxy.registry.SecretEntry;

import java.util.HashMap;
import java.util.Map;
/**
 * Holds information about an MCP session.
 * @author laurent
 */
public class SessionInfo {

   private final String id;
   private final String serviceId;
   private final String protocolVersion;

   private final Map<SecretEntry, String> elicitationSecretValue = new HashMap<>();

   /**
    * Create a new SessionInformation instance with mandatory fields.
    * @param id The session unique identifier
    * @param serviceId The service identifier associated with the session
    * @param protocolVersion The protocol version used in the session
    */
   public SessionInfo(String id, String serviceId, String protocolVersion) {
      this.id = id;
      this.serviceId = serviceId;
      this.protocolVersion = protocolVersion;
   }

   public String getId() {
      return id;
   }
   public String getServiceId() {
      return serviceId;
   }
   public String getProtocolVersion() {
      return protocolVersion;
   }

   public void setSecretValue(SecretEntry secretEntry, String value) {
      elicitationSecretValue.put(secretEntry, value);
   }
   public String getSecretValue(SecretEntry secretEntry) {
      return elicitationSecretValue.get(secretEntry);
   }

   public void removeSecretValue(SecretEntry secretEntry) {
      elicitationSecretValue.remove(secretEntry);
   }
}
