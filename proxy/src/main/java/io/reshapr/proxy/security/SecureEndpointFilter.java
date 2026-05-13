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
package io.reshapr.proxy.security;

import io.reshapr.proxy.registry.ConfigurationEntry;
import io.reshapr.proxy.registry.GatewayRegistry;
import io.reshapr.proxy.registry.OAuth2ConfigurationEntry;
import io.reshapr.proxy.registry.ServiceEntry;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.util.List;
import java.util.Set;

/**
 * SecureEndpointFilter is a JAX-RS filter that applies security checks to incoming requests.
 * The filter can be used to enforce security policies, such as authentication and authorization.
 * @author laurent
 */
@Provider
@SecureEndpoint
public class SecureEndpointFilter implements ContainerRequestFilter {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   private static final String MCP_PATH_PREFIX = "/mcp/";
   private static final String API_KEY_HEADER = "x-reshapr-key";

   /** Request context property key for the authenticated user ID. */
   public static final String USER_ID_PROPERTY = "reshapr.auth.userId";

   private static final Set<JWSAlgorithm> JWS_SUPPORTED_ALGORITHMS = Set.of(
         JWSAlgorithm.RS256,
         JWSAlgorithm.RS384,
         JWSAlgorithm.RS512,
         JWSAlgorithm.PS256,
         JWSAlgorithm.PS384,
         JWSAlgorithm.PS512
   );
   private static final Set<String> JWT_VERIFIED_CLAIMS = Set.of(
         JWTClaimNames.SUBJECT,
         JWTClaimNames.ISSUED_AT,
         JWTClaimNames.EXPIRATION_TIME,
         JWTClaimNames.JWT_ID
   );

   private final GatewayRegistry gatewayRegistry;

   @ConfigProperty(name = "reshapr.gateway.fqdns", defaultValue = "localhost:7777")
   List<String> fqdns;

   public SecureEndpointFilter(GatewayRegistry gatewayRegistry) {
      this.gatewayRegistry = gatewayRegistry;
   }

   @Override
   public void filter(ContainerRequestContext ctx) throws IOException {
      String path = ctx.getUriInfo().getPath();
      if (path.startsWith(MCP_PATH_PREFIX)) {
         // This is a protected endpoint, we can apply security checks here.
         logger.debugf("Applying security checks for path: '%s'", path);

         // Remove "/mcp/" prefix and extract the ServiceEntry.
         ServiceEntry service = null;
         String shortPath = path.substring(MCP_PATH_PREFIX.length());
         String[] parts = shortPath.split("/");

         if (parts.length == 1) {
            service = gatewayRegistry.getService(parts[0]);
         } else if (parts.length == 3) {
            // If serviceName was encoded with '+' instead of '%20', remove them.
            if (parts[1].contains("+")) {
               parts[1] = parts[1].replace('+', ' ');
            }
            service = gatewayRegistry.getService(parts[0], parts[1], parts[2]);
         }

         if (service != null) {
            ConfigurationEntry configuration = gatewayRegistry.getConfiguration(service);

            // Do the security checks if any.
            if (isSecuredService(configuration)) {
               if (isSecuredWithAPIKey(configuration)) {
                  checkAPIKeyValidity(configuration, ctx);
               } else if (isSecuredWithOAuth2(configuration)) {
                  checkOAuth2Validity(service, configuration, ctx);
               }
            }
         }
      }
   }

   private boolean isSecuredService(ConfigurationEntry configuration) {
      return configuration != null &&
            (configuration.apiKey() != null || configuration.oauth2Configuration() != null);
   }

   private boolean isSecuredWithAPIKey(ConfigurationEntry configuration) {
      return (configuration.apiKey() != null && !configuration.apiKey().isEmpty());
   }

   private boolean isSecuredWithOAuth2(ConfigurationEntry configuration) {
      return (configuration.oauth2Configuration() != null && !configuration.oauth2Configuration().authorizationServers().isEmpty());
   }

   private void checkAPIKeyValidity(ConfigurationEntry configuration, ContainerRequestContext ctx) {
      // Check for API key in headers.
      String apiKey = ctx.getHeaderString(API_KEY_HEADER);
      boolean valid =  configuration.apiKey() != null && configuration.apiKey().equals(apiKey);
      if (!valid) {
         logger.warnf("Invalid or missing API key for configuration with ID: '%s'", configuration.id());
         ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
      }
   }

   private void checkOAuth2Validity(ServiceEntry service, ConfigurationEntry configuration, ContainerRequestContext ctx) {
      String authorizationHeader = ctx.getHeaderString(HttpHeaders.AUTHORIZATION);
      if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
         logger.warnf("Missing or invalid Authorization header for configuration with ID: '%s'", configuration.id());
         ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
               .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer resource_metadata=https://" + fqdns.getFirst() + "/.well-known/oauth-protected-resource" + ctx.getUriInfo().getPath())
               .build());
         return;
      }

      // Continue with OAuth2 token validation.
      final OAuth2ConfigurationEntry oauth2Config = configuration.oauth2Configuration();
      URL jwksUri = null;
      try {
         jwksUri = URI.create(oauth2Config.jwksUri()).toURL();
      } catch (Exception e) {
         logger.errorf("Invalid JWK Set URL in OAuth2 configuration: '%s'", oauth2Config.jwksUri());
         ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
         return;
      }
      String token = authorizationHeader.substring("Bearer ".length());

      // Here you would typically validate the token against the OAuth2 server.
      logger.debugf("OAuth2 token received: %s", token);

      // Create a JWT processor for the access tokens
      ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();

      // Configure the JWT processor with a key selector to feed matching public
      // RSA keys sourced from the JWK set URL.
      JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(
            JWS_SUPPORTED_ALGORITHMS,
            JWKSourceBuilder.create(jwksUri).retrying(true).build());
      jwtProcessor.setJWSKeySelector(keySelector);

      // Set the required JWT claims for access tokens
      jwtProcessor.setJWTClaimsSetVerifier(new MultipleIssuerClaimsVerifier(
            oauth2Config.authorizationServers(),
            JWT_VERIFIED_CLAIMS
      ));

      JWTClaimsSet claimsSet;
      SecurityContext securityCtx = null;
      try {
         claimsSet = jwtProcessor.process(token, securityCtx);
      } catch (ParseException | BadJOSEException e) {
         // Malformed token.
         logger.warnf("Bad OAuth2 token received: %s", e.getMessage());
         ctx.abortWith(Response.status(Response.Status.BAD_REQUEST).build());
         return;
      } catch (JOSEException e) {
         // Key sourcing failed or another internal exception.
         logger.warnf("Invalid OAuth2 token received: %s", e.getMessage());
         ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
         return;
      }

      // Now check the claimsSet for resource as per https://modelcontextprotocol.io/specification/2025-06-18/basic/authorization#token-handling
      try {
         String resource = claimsSet.getClaimAsString("resource");
         if (resource != null && !resource.equalsIgnoreCase("https://" + fqdns.getFirst() + ctx.getUriInfo().getPath())) {
            logger.warnf("Invalid OAuth2 token received, resource claim does not match '%s'", "https://" + fqdns.getFirst() + ctx.getUriInfo().getPath());
            ctx.abortWith(Response.status(Response.Status.FORBIDDEN).build());
            return;
         }
      } catch (ParseException pe) {
         // Malformed token.
         logger.warnf("Bad OAuth2 token received, resource claim cannot be parsed as String", pe);
         ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
         return;
      }

      // If issued by the Reshapr internal IDP, we can also check the serviceID claim.
      try {
         String serviceID = claimsSet.getClaimAsString("serviceId");
         if (serviceID != null && !serviceID.equals(service.id())) {
            logger.warnf("Invalid OAuth2 token received, serviceId claim does not match service ID '%s'", service.id());
            ctx.abortWith(Response.status(Response.Status.FORBIDDEN).build());
            return;
         }
      } catch (ParseException pe) {
         // Malformed token.
         logger.warnf("Bad OAuth2 token received, serviceId claim cannot be parsed as String", pe);
         ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
         return;
      }

      // If the configuration has scopes, check they are present in the token.
      if (oauth2Config.scopes() != null && !oauth2Config.scopes().isEmpty()) {
         List<String> tokenScopes;

         try {
            var scopeClaim = claimsSet.getStringClaim("scope");
            if (scopeClaim == null) {
               scopeClaim = claimsSet.getStringClaim("scp");
            }
            if (scopeClaim != null) {
               tokenScopes = List.of(scopeClaim.split(" "));
            } else {
               tokenScopes = claimsSet.getStringListClaim("scope");
               if (tokenScopes == null) {
                  tokenScopes = claimsSet.getStringListClaim("scp");
               }
            }
         } catch (ParseException pe) {
            // Malformed token.
            logger.warnf("Bad OAuth2 token received, scope claim cannot be parsed as String or List<String>", pe);
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            return;
         }

         if (tokenScopes == null || tokenScopes.isEmpty()) {
            logger.warnf("Invalid OAuth2 token received, no scope claim found but expected: '%s'", String.join(" ", oauth2Config.scopes()));
            ctx.abortWith(Response.status(Response.Status.FORBIDDEN).build());
            return;
         }
          for (String expectedScope : oauth2Config.scopes()) {
             if (!tokenScopes.contains(expectedScope)) {
                logger.warnf("Invalid OAuth2 token received, scope claim does not contain expected scope: '%s'", expectedScope);
                ctx.abortWith(Response.status(Response.Status.FORBIDDEN).build());
                return;
             }
          }
       }

       // Store authenticated user ID (JWT subject) in request context for downstream audit use.
       String subject = claimsSet.getSubject();
       if (subject != null) {
          ctx.setProperty(USER_ID_PROPERTY, subject);
       }
    }

   /** Default JOSE verifies allows only exact match on issuers. This verifier allows multiple issuers. */
   static class MultipleIssuerClaimsVerifier extends DefaultJWTClaimsVerifier<SecurityContext> {
      private final List<String> expectedIssuers;

      public MultipleIssuerClaimsVerifier(List<String> expectedIssuers, Set<String> requiredClaims) {
         super(null, requiredClaims);
         this.expectedIssuers = expectedIssuers;
      }

      @Override
      public void verify(JWTClaimsSet claimsSet, SecurityContext context) throws BadJWTException {
         super.verify(claimsSet, context);
         // Verify that the issuer matches one of the configured authorization servers.
         String issuer = claimsSet.getIssuer();
         if (issuer == null || !expectedIssuers.contains(issuer)) {
            throw new BadJWTException("JWT issuer '" + issuer + "' does not match any configured authorization server");
         }
      }
   }
}
