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
package io.reshapr.util;

/**
 * A lightweight representation of HTTP authentication credentials used by {@link HttpDownloader}.
 * <p>
 * This record carries only the fields relevant for HTTP authentication and SSL configuration,
 * decoupled from any persistence layer. Callers should map from their domain secret entity
 * to this record before invoking downloader methods.
 *
 * @param username    the username for Basic authentication (may be {@code null})
 * @param password    the password for Basic authentication (may be {@code null})
 * @param token       the authentication token (may be {@code null})
 * @param tokenHeader the custom header name for the token; if {@code null} or blank,
 *                    the token is sent as a Bearer token in the Authorization header
 * @param certPem     a PEM-encoded CA certificate for custom SSL trust (may be {@code null})
 * @author laurent
 */
public record HttpSecret(
      String username,
      String password,
      String token,
      String tokenHeader,
      String certPem
) {
}

