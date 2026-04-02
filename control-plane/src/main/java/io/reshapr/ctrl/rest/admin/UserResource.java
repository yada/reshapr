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

import io.reshapr.ctrl.model.Organization;
import io.reshapr.ctrl.model.User;
import io.reshapr.ctrl.model.UserStatus;
import io.reshapr.ctrl.repository.OrganizationRepository;
import io.reshapr.ctrl.repository.UserRepository;
import io.reshapr.ctrl.security.AdminAuthenticated;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;

@RunOnVirtualThread
@Path("/api/admin/users")
@AdminAuthenticated
public class UserResource {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   private final UserRepository userRepository;
   private final OrganizationRepository organizationRepository;

   /**
    * Build a UserResource with required dependencies.
    * @param userRepository The User repository
    * @param organizationRepository The Organization repository
    */
   public UserResource(UserRepository userRepository, OrganizationRepository organizationRepository) {
      this.userRepository = userRepository;
      this.organizationRepository = organizationRepository;
   }

   @POST
   @Transactional
   public Response createUser(UserDTO userDTO) {
      logger.infof("Creating user with username: %s", userDTO.username());

      // Check if user already exists.
      User user = userRepository.findByUsername(userDTO.username());
      if (user != null) {
         logger.warnf("User with username %s already exists", userDTO.username());
         return Response.status(Response.Status.CONFLICT.getStatusCode(), "User already exists").build();
      }

      // Create and persist user.
      user = new User();
      user.username = userDTO.username();
      user.email = userDTO.email();
      if (userDTO.password() != null && !userDTO.password().isBlank()) {
         user.password = BcryptUtil.bcryptHash(userDTO.password());
      }
      if (userDTO.firstname() != null) {
         user.firstname = userDTO.firstname();
      }
      if (userDTO.lastname() != null) {
         user.lastname = userDTO.lastname();
      }
      user.status = UserStatus.REGISTERED;
      userRepository.persistAndFlush(user);

      return Response.status(Response.Status.CREATED).entity(user).build();
   }

   @POST
   @Path("/{username}/organization")
   @Transactional
   public Response createOrganization(@PathParam("username") String username, @Valid OrganizationDTO organizationDTO) {
      logger.infof("Creating organization %s for user %s", organizationDTO.name(), username);

      // Find user by username.
      User user = userRepository.findByUsername(username);
      if (user == null) {
         logger.warnf("User with username %s not found", username);
         return Response.status(Response.Status.NOT_FOUND.getStatusCode(), "User not found").build();
      }

      // Check if organization already exists.
      Organization organization = organizationRepository.findByName(organizationDTO.name());
      if (organization != null) {
         logger.warnf("Organization with name %s already exists", organizationDTO.name());
         return Response.status(Response.Status.CONFLICT.getStatusCode(), "Organization already exists").build();
      }

      // Create and persist organization.
      organization = new Organization();
      organization.name = organizationDTO.name();
      organization.owner = user;
      organizationRepository.persistAndFlush(organization);

      // Assign organization to user.
      user.organizations.add(organization);
      if (user.defaultOrganization == null) {
         user.defaultOrganization = organization;
      }
      userRepository.persistAndFlush(user);

      return Response.status(Response.Status.CREATED).entity(organizationDTO).build();
   }

   @PUT
   @Path("/{username}/organization/{organizationName}")
   @Transactional
   public Response updateOrganization(@PathParam("username") String username, @PathParam("organizationName") String organizationName,
                                      @Valid OrganizationDTO organizationDTO) {
      logger.infof("Updating organization %s for user %s", organizationName, username);

      // Find user by username.
      User user = userRepository.findByUsername(username);
      if (user == null) {
         logger.warnf("User with username %s not found", username);
         return Response.status(Response.Status.NOT_FOUND.getStatusCode(), "User not found").build();
      }

      // Find organization by name.
      Organization organization = organizationRepository.findByName(organizationName);
      if (organization == null || !organization.owner.username.equals(username)) {
         logger.warnf("Organization with name %s not found or not owned by user %s", organizationName, username);
         return Response.status(Response.Status.NOT_FOUND.getStatusCode(), "Organization not found or not owned by user").build();
      }

      // Update and persist organization.
      organization.name = organizationDTO.name();
      organization.description = organizationDTO.description();
      organization.icon = organizationDTO.icon();
      organizationRepository.persistAndFlush(organization);

      return Response.ok(organizationDTO).build();
   }

   @PUT
   @Path("/{username}/organization/{organizationName}/owner")
   @Transactional
   public Response updateOrganizationOwner(@PathParam("username") String username, @PathParam("organizationName") String organizationName) {
      logger.infof("Updating organization owner for organization %s to user %s", organizationName, username);

      // Find user by username.
      User user = userRepository.findByUsername(username);
      if (user == null) {
         logger.warnf("User with username %s not found", username);
         return Response.status(Response.Status.NOT_FOUND.getStatusCode(), "User not found").build();
      }

      // Find organization by name.
      Organization organization = organizationRepository.findByName(organizationName);
      if (organization == null) {
         logger.warnf("Organization with name %s not found", organizationName);
         return Response.status(Response.Status.NOT_FOUND.getStatusCode(), "Organization not found").build();
      }

      // Update and persist user and organization.
      organization.owner = user;
      organizationRepository.persistAndFlush(organization);

      // Assign organization to user.
      user.organizations.add(organization);
      if (user.defaultOrganization == null) {
         user.defaultOrganization = organization;
      }
      userRepository.persistAndFlush(user);

      return Response.ok(new OrganizationDTO(organizationName, organization.description, organization.icon)).build();
   }

   @PUT
   @Path("/{username}/memberships")
   @Transactional
   public Response assignMembership(@PathParam("username") String username, List<String> organisationIds) {
      logger.infof("Assigning memberships %s to user %s", organisationIds, username);

      // Find user by username.
      User user = userRepository.findByUsername(username);
      if (user == null) {
         logger.warnf("User with username %s not found", username);
         return Response.status(Response.Status.NOT_FOUND.getStatusCode(), "User not found").build();
      }

      // Clear existing organizations and assign new ones.
      user.organizations.clear();
      user.organizations = organizationRepository.findByNames(organisationIds);
      userRepository.persistAndFlush(user);

      return Response.ok(organisationIds).build();
   }
}
