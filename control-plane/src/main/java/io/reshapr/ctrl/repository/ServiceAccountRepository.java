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
package io.reshapr.ctrl.repository;

import io.reshapr.ctrl.model.ServiceAccount;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * ServiceAccountRepository is a repository interface for managing ServiceAccount entities.
 * @author laurent
 */
@ApplicationScoped
public class ServiceAccountRepository implements PanacheRepositoryBase<ServiceAccount, String> {

   /**
    * Finds a ServiceAccount by its name.
    * @param name the name of the ServiceAccount
    * @return the ServiceAccount entity if found, otherwise null
    */
   public ServiceAccount findByName(String name) {
      return find("name", name).firstResult();
   }

   /**
    * Finds a ServiceAccount by its Kubernetes subject.
    * @param subject the Kubernetes subject of the ServiceAccount
    * @return the ServiceAccount entity if found, otherwise null
    */
   public ServiceAccount findByK8sSubject(String subject) {
      return find("k8sSubject", subject).firstResult();
   }
}
