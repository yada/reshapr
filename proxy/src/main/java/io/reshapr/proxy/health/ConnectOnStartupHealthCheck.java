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
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 * Health check that verifies that the gateway can connect to the control plane on startup.
 * This is used to ensure that the proxy is healthy and has fetched initial data from the control plane
 * before it starts accepting traffic.
 * @author laurent
 */
@Readiness
@ApplicationScoped
public class ConnectOnStartupHealthCheck implements HealthCheck {

   private final ReshaprGatewayApp reshaprGatewayApp;

   /**
    *
    * @param reshaprGatewayApp
    */
   public ConnectOnStartupHealthCheck(ReshaprGatewayApp reshaprGatewayApp) {
      this.reshaprGatewayApp = reshaprGatewayApp;
   }

   @Override
   public HealthCheckResponse call() {
      return HealthCheckResponse.named("ConnectOnStartupHealthCheck")
            .status(reshaprGatewayApp.hasConnectedToControlPlane())
            .build();
   }
}
