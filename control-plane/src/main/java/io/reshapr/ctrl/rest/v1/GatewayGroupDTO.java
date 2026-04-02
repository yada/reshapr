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

import java.util.Map;

/**
 * A record representing a gateway group in the Reshapr REST API v1.
 * @param id The unique identifier of the gateway group.
 * @param organizationId The identifier of the organization to which the gateway group belongs.
 * @param name The name of the gateway group.
 * @param labels A map of labels associated with the gateway group, allowing for flexible metadata storage.
 * @author laurent
 */
@RegisterForReflection
public record GatewayGroupDTO(
      String id,
      String organizationId,
      @Size(max = 255, message = "Name must not exceed 255 characters")
      @JsonDeserialize(using = HtmlEncodedStringDeserializer.class)
      String name,
      Map<String, String> labels) {
}
