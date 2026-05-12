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
 * Represents a third-party OAuth2 configuration entry in the registry.
 * @param clientId The OAuth2 client ID
 * @param clientSecret The OAuth2 client secret if any
 * @param authorizationEndpoint The OAuth2 authorization endpoint URL
 * @param tokenEndpoint The OAuth2 token endpoint URL
 * @author laurent
 */
public record OAuth2ClientConfigurationEntry(
      String clientId,
      String clientSecret,
      String authorizationEndpoint,
      String tokenEndpoint) {
}
