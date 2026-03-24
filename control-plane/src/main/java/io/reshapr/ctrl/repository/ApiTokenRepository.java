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

import io.reshapr.ctrl.model.ApiToken;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Repository for managing API tokens in the Reshapr control plane.
 * @author laurent
 */
@ApplicationScoped
public class ApiTokenRepository implements PanacheRepositoryBase<ApiToken, String> {

   public ApiToken findByToken(String token) {
      return find("token", token).firstResult();
   }

   public ApiToken findByNameAndOrganizationId(String name, String organizationId) {
      return find("name = ?1 and organizationId = ?2", name, organizationId).firstResult();
   }

   public List<ApiToken> findByOrganizationId(String organizationId) {
      return find("from ApiToken t left join fetch t.user where organizationId = ?1", organizationId).list();
   }
}
