/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.ozone.s3.endpoint.vectors.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public class QueryOutputVector {
  @JsonProperty(value = "key", required = true)
  private String key;

  @JsonProperty(value = "data")
  private VectorData data;

  @JsonProperty(value = "distance")
  private float distance;

  @JsonProperty(value = "metadata")
  private JsonNode metadata;

  @JsonCreator
  public QueryOutputVector(@JsonProperty("data") VectorData data, @JsonProperty("distance") float distance,
      @JsonProperty(value = "key", required = true) String key, @JsonProperty("metadata") JsonNode metadata) {
    this.data = data;
    this.distance = distance;
    this.key = key;
    this.metadata = metadata;
  }
}
