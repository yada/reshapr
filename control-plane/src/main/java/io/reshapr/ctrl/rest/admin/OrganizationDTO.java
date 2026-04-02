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
package io.reshapr.ctrl.rest.admin;

import io.reshapr.json.HtmlEncodedStringDeserializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for Organization information.
 * @param name The name of the organization
 * @param description A brief description of the organization
 * @param icon URL or path to the organization's icon
 */
@RegisterForReflection
public record OrganizationDTO(
      @NotBlank(message = "Organization name must not be blank")
      @Size(min = 1, max = 100, message = "Organization name must be between 1 and 100 characters")
      @Pattern(regexp = "[a-zA-Z0-9_]+", message = "Organization name must only contain alphanumeric characters and underscores")
      String name,
      @Size(max = 255, message = "Description must not exceed 255 characters")
      @JsonDeserialize(using = HtmlEncodedStringDeserializer.class)
      String description,
      String icon
) {
}
