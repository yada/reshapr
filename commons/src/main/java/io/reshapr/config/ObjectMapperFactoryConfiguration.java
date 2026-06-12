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
package io.reshapr.config;

import io.reshapr.json.ObjectMapperFactory;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Optional;

/**
 * Configuration class to set up ObjectMapperFactory options based on application properties.
 * This allows customization of the Yaml Object mapper's behavior, such as maximum characters.
 * @author laurent
 */
@ApplicationScoped
public class ObjectMapperFactoryConfiguration {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   @ConfigProperty(name = "quarkus.http.limits.max-body-size")
   Optional<String> maxBodySize;

   void startup(@Observes StartupEvent event) {
      logger.debugf("Configuring ObjectMapperFactory with maxBodySize=%s",
            maxBodySize.orElse(null));
      // Configure the ObjectMapperFactory with the one defined in application.properties.
      maxBodySize.ifPresent(ObjectMapperFactory::configureMaxFileSize);
   }
}
