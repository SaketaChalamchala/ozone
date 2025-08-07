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

package org.apache.hadoop.ozone.s3.endpoint.vectors.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.hadoop.ozone.s3.endpoint.vectors.data.VectorData;

public class QueryVectorRequest {
  @JsonProperty(value = "indexArn", required = true)
  private String indexArn;
  @JsonProperty(value = "vectorBucketName")
  private String vectorBucketName;
  @JsonProperty(value = "indexName", required = true)
  private String indexName;
  @JsonProperty(value = "queryVector", required = true)
  private VectorData queryVector;
  @JsonProperty(value = "topK", required = true)
  private int topK;
  @JsonProperty(value = "returnDistance", required = false)
  private boolean returnDistance;
  @JsonProperty(value = "returnMetadata", required = false)
  private boolean returnMetadata;

  @JsonCreator
  public QueryVectorRequest(@JsonProperty(value = "indexArn", required = true) String indexArn,
      @JsonProperty(value = "vectorBucketName") String vectorBucketName,
      @JsonProperty(value = "indexName", required = true) String indexName,
      @JsonProperty(value = "queryVector", required = true) VectorData queryVector,
      @JsonProperty(value = "returnDistance", required = false) boolean returnDistance,
      @JsonProperty(value = "returnMetadata", required = false) boolean returnMetadata,
      @JsonProperty(value = "topK", required = true) int topK) {
    this.indexArn = indexArn;
    this.indexName = indexName;
    this.queryVector = queryVector;
    this.returnDistance = returnDistance;
    this.returnMetadata = returnMetadata;
    this.topK = topK;
    this.vectorBucketName = vectorBucketName;
  }

  public String getIndexArn() {
    return indexArn;
  }

  public String getIndexName() {
    return indexName;
  }

  public VectorData getQueryVector() {
    return queryVector;
  }

  public boolean isReturnDistance() {
    return returnDistance;
  }

  public boolean isReturnMetadata() {
    return returnMetadata;
  }

  public int getTopK() {
    return topK;
  }

  public String getVectorBucketName() {
    return vectorBucketName;
  }
}
