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
package io.reshapr.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;

/**
 * Validator for the {@link HttpUrl} constraint. Ensures that the value is a well-formed
 * URL with an {@code http} or {@code https} scheme. This rejects dangerous URI schemes
 * such as {@code javascript:}, {@code data:}, or {@code vbscript:}.
 * @author laurent
 */
public class HttpUrlValidator implements ConstraintValidator<HttpUrl, String> {
   @Override
   public boolean isValid(String value, ConstraintValidatorContext context) {
      if (value == null || value.isBlank()) {
         // Null/blank values are valid; use @NotBlank to enforce presence.
         return true;
      }
      try {
         URI uri = URI.create(value);
         String scheme = uri.getScheme();
         return scheme != null
               && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));
      } catch (IllegalArgumentException e) {
         return false;
      }
   }
}
