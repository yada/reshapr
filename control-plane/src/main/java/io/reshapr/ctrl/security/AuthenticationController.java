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
package io.reshapr.ctrl.security;

import io.reshapr.ctrl.model.User;
import io.reshapr.ctrl.repository.UserRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.jwt.build.Jwt;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;

/**
 * Controller for handling authentication and user profile related requests.
 * @author laurent
 */
@RunOnVirtualThread
@Path("/auth")
public class AuthenticationController {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   public static final String RESHAPR_IDENTITY_PROVIDER = "reshapr";

   private final UserRepository userRepository;

   @Inject
   private ObjectMapper objectMapper;

   @ConfigProperty(name = "reshapr.ctrl.public-url")
   String reshaprCtrlPublicUrl;


   /**
    * Creates a new AuthenticationController with required dependencies.
    * @param userRepository The repository to access user data.
    */
   public AuthenticationController(UserRepository userRepository) {
      this.userRepository = userRepository;
   }

   @POST
   @Path("/login/reshapr")
   public Response loginWithReshpar(LoginRequest loginRequest) {
      logger.infof("AuthenticationController.loginWithReshapr() called with username: %s", loginRequest.username);

      // Validate the user credentials.
      var user = userRepository.findByUsername(loginRequest.username);
      if (user == null || !user.verifyPassword(loginRequest.password)) {
         logger.warnf("Authentication failed for user: %s", loginRequest.username);
         return Response.status(Response.Status.UNAUTHORIZED).build();
      }

      // Generate a token for the authenticated user
      String token = generateTokenForUser(RESHAPR_IDENTITY_PROVIDER, user, user.defaultOrganization.name);
      logger.infof("Authentication successful for user: %s", loginRequest.username);

      return Response.ok(token).build();
   }

   @POST
   @Path("/login/token")
   @Produces(MediaType.TEXT_PLAIN)
   @AdminAuthenticated
   public Response generateLoginToken(DelegatedLoginRequest loginRequest) {
      logger.infof("Delegated login token requested for user: %s", loginRequest.username);

      // Find user by username.
      User user = userRepository.findByUsername(loginRequest.username);
      if (user == null) {
         logger.warnf("User with username %s not found", loginRequest.username);
         return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
      }

      if (user.defaultOrganization == null) {
         logger.warnf("User %s has no default organization", loginRequest.username);
         return Response.status(Response.Status.BAD_REQUEST).entity("User has no organization").build();
      }

      // Generate a JWT token for the user.
      String token = generateTokenForUser("delegated", user, user.defaultOrganization.name);

      logger.infof("Delegated login token generated for user: %s (org: %s)", user.username, user.defaultOrganization.name);
      return Response.ok(token).build();
   }

   @POST
   @Authenticated
   @Path("switchOrganization/{organizationId}")
   public Response switchOrganization(@PathParam("organizationId") String organizationId) {
      logger.infof("AuthenticationController.switchOrganization() called with organizationId: %s", organizationId);

//      // Get the current user from the security context
//      User currentUser = userRepository.getCurrentUser();
//      if (currentUser == null) {
//         logger.warn("No authenticated user found.");
//         return Response.status(Response.Status.UNAUTHORIZED).build();
//      }
//
//      // Switch the user's default organization
//      currentUser.setDefaultOrganization(organizationId);
//      userRepository.update(currentUser);
//
//      // Generate a new token for the user with the updated organization
//      String token = generateTokenForUser(RESHAPR_IDENTITY_PROVIDER, currentUser);
//      logger.infof("Switched organization for user: %s to %s", currentUser.username, organizationId);

      return Response.ok().build();
   }

   public record LoginRequest(String username, String password) {}

   public record DelegatedLoginRequest(String username) {}

   private String generateTokenForUser(String authorityId, User user, String organizationId) {
      // Generate a Jwt with user information and set is as a cookie.
      String token = Jwt.issuer("https://app.reshapr.io")
            .subject(user.username)
            .upn(user.username)
            .groups("user")
            .expiresIn(Duration.ofHours(2))
            .claim("org", organizationId)
            .claim("email", user.email)
            .sign();

      return token;
   }
}
