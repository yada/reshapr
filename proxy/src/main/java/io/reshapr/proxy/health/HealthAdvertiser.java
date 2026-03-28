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
package io.reshapr.proxy.health;

import io.reshapr.proxy.ReshaprGatewayApp;
import io.reshapr.health.gateway.v1.GatewayHealthResponse;
import io.reshapr.health.gateway.v1.GatewayHealthServiceGrpc;
import io.reshapr.health.gateway.v1.GatewayRequest;
import io.reshapr.proxy.security.GrpcAuthClientInterceptor;

import io.quarkus.arc.Unremovable;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.RegisterClientInterceptor;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Advertises the health status of the Reshapr Gateway to the control plane.
 * It periodically sends a heartbeat to indicate that the gateway is healthy.
 * On shutdown, it advertises that the gateway is shutting down.
 * @author laurent
 */
@Unremovable
@ApplicationScoped
public class HealthAdvertiser {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   @ConfigProperty(name = "reshapr.gateway.id")
   String gatewayId;

   private final GatewayHealthServiceGrpc.GatewayHealthServiceBlockingStub healthService;
   private final ReshaprGatewayApp reshaprGatewayApp;

   /***
    *
    * @param healthService
    * @param reshaprGatewayApp
    */
   public HealthAdvertiser(@RegisterClientInterceptor(GrpcAuthClientInterceptor.class) @GrpcClient("gateway-health") GatewayHealthServiceGrpc.GatewayHealthServiceBlockingStub healthService,
                           ReshaprGatewayApp reshaprGatewayApp) {
      this.healthService = healthService;
      this.reshaprGatewayApp = reshaprGatewayApp;
   }

   @Scheduled(every="2m", delayed="5s")
   void advertiseHealth() {
      GatewayRequest request = GatewayRequest.newBuilder().setGatewayId(gatewayId).build();

      try {
         GatewayHealthResponse response = this.healthService.advertHealthy(request);
         logger.debugf("Health advertisement successful for gateway ID: %s", gatewayId);
         if (!response.getAcknowledged()) {
            logger.warnf("Health advertisement not acknowledged for gateway ID: %s", gatewayId);
            registerGatewayAgain();
         }
      } catch (Exception e) {
         logger.errorf("Failed to advertise health for gateway ID %s: %s", gatewayId, e.getMessage());
      }
   }

   @PreDestroy
   void advertiseShutdown() {
      logger.infof("Shutting down Reshapr Gateway with ID: %s", gatewayId);
      GatewayRequest request = GatewayRequest.newBuilder().setGatewayId(gatewayId).build();

      try {
         GatewayHealthResponse response = this.healthService.advertShutdown(request);
         logger.debugf("Shutdown advertisement successful for gateway ID: %s", gatewayId);
      } catch (Exception e) {
         logger.warnf("Failed to advertise shutdown for gateway ID %s: %s", gatewayId, e.getMessage());
         logger.warn("No worry: the control plane will clean-up the gateway registration if not back before next cycle.");
      }
   }

   void registerGatewayAgain() {
      logger.warnf("Gateway may have been disconnected from control plane for too long. Proceeding to a new registration attempt.");
      try {
         reshaprGatewayApp.registerAndDiscoverExpositions();
      } catch (Throwable t) {
         logger.errorf("Failed to re-register gateway %s: %s", gatewayId, t.getMessage());
      }
   }
}
