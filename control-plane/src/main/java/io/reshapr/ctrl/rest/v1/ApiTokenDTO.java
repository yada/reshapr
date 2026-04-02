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
package io.reshapr.ctrl.rest.v1;

import io.reshapr.json.HtmlEncodedStringDeserializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) for an API token in the Reshapr control plane.
 * @author laurent
 */
@RegisterForReflection
public class ApiTokenDTO {

   protected String id;
   protected String organizationId;
   @Size(max = 255, message = "Name must not exceed 255 characters")
   @JsonDeserialize(using = HtmlEncodedStringDeserializer.class)
   protected String name;
   // Should only be set after creation of the token.
   protected String token;
   protected LocalDateTime validUntil;
   protected String username;

   public ApiTokenDTO() {
   }

   public ApiTokenDTO(String id, String organizationId, String name, LocalDateTime validUntil, String username) {
      this.id = id;
      this.organizationId = organizationId;
      this.name = name;
      this.validUntil = validUntil;
      this.username = username;
   }

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public String getOrganizationId() {
      return organizationId;
   }

   public void setOrganizationId(String organizationId) {
      this.organizationId = organizationId;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getToken() {
      return token;
   }

   public void setToken(String token) {
      this.token = token;
   }

   public LocalDateTime getValidUntil() {
      return validUntil;
   }

   public void setValidUntil(LocalDateTime validUntil) {
      this.validUntil = validUntil;
   }

   public String getUsername() {
      return username;
   }

   public void setUsername(String username) {
      this.username = username;
   }
}
