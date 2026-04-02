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

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validation constraint annotation that ensures a string value is a valid HTTP or HTTPS URL.
 * This prevents injection of dangerous URI schemes such as {@code javascript:}, {@code data:},
 * or {@code vbscript:}.
 * <p>
 * Null values are considered valid (use {@code @NotBlank} to reject nulls).
 * <pre>
 * {@code @HttpUrl}
 * String authorizationEndpoint
 * </pre>
 * @author laurent
 */
@Documented
@Constraint(validatedBy = HttpUrlValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpUrl {

   String message() default "Must be a valid HTTP or HTTPS URL";

   Class<?>[] groups() default {};

   Class<? extends Payload>[] payload() default {};
}

