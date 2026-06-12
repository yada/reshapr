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
package io.reshapr.json;

/**
 * Constants definition of JsonSchema syntax elements.
 * @author laurent
 */
public class JsonSchemaElements {

   public static final String JSON_V12_SCHEMA_IDENTIFIER = "http://json-schema.org/draft/2020-12/schema#";
   public static final String JSON_SCHEMA_IDENTIFIER_ELEMENT = "$schema";

   public static final String JSON_SCHEMA_COMPONENTS_ELEMENT = "components";
   public static final String JSON_SCHEMA_TYPE_ELEMENT = "type";
   public static final String JSON_SCHEMA_PROPERTIES_ELEMENT = "properties";
   public static final String JSON_SCHEMA_REQUIRED_ELEMENT = "required";
   public static final String JSON_SCHEMA_ITEMS_ELEMENT = "items";
   public static final String JSON_SCHEMA_ADD_PROPERTIES_ELEMENT = "additionalProperties";

   public static final String JSON_SCHEMA_OBJECT_TYPE = "object";
   public static final String JSON_SCHEMA_ARRAY_TYPE = "array";
   public static final String JSON_SCHEMA_ENUM_TYPE = "enum";
}
