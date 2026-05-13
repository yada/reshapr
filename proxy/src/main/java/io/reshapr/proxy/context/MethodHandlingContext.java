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
package io.reshapr.proxy.context;

/**
 * MethodHandlingContext is a utility class that provides a scoped storage for MethodHandlingInfo.
 * @author laurent
 */
public class MethodHandlingContext {

   public static final ScopedValue<MethodHandlingInfo> METHOD_HANDLING_INFO = ScopedValue.newInstance();

   private MethodHandlingContext() {
      // Utility class
   }

   public static String getRemoteAddress() {
      return METHOD_HANDLING_INFO.get().remoteAddress();
   }

   public static SessionInfo getSessionInfo() {
      return METHOD_HANDLING_INFO.get().mcpSessionInfo();
   }

   public static String getUserId() {
      return METHOD_HANDLING_INFO.get().userId();
   }
}
