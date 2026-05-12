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
package io.reshapr.proxy.registry;

/**
 * Represents a secret entry in the registry.
 * This record holds various types of secrets such as name, username, password, token, token header, and certificate PEM.
 * @param name The name of the secret.
 * @param username The username associated with the secret.
 * @param password The password associated with the secret.
 * @param token An optional token associated with the secret.
 * @param tokenHeader An optional header for the token.
 * @param certPem An optional PEM-encoded certificate associated with the secret.
 * @param useElicitation A flag indicating whether elicitation is used for this secret.
 * @param oauth2ClientConfiguration An optional OAuth2 client configuration entry.
 * @author laurent
 */
public record SecretEntry(
      String name,
      String username,
      String password,
      String token,
      String tokenHeader,
      String certPem,
      boolean useElicitation,
      OAuth2ClientConfigurationEntry oauth2ClientConfiguration) {
}
