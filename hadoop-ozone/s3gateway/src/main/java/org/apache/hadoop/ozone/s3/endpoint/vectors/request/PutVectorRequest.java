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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.apache.hadoop.ozone.s3.endpoint.vectors.data.PutInputVector;
import org.apache.hadoop.ozone.s3.endpoint.vectors.data.VectorData;

public class PutVectorRequest {
  @JsonProperty(value = "vectorBucketName")
  private String vectorBucketName;
  @JsonProperty(value = "indexName")
  private String indexName;
  @JsonProperty(value = "indexArn")
  private String indexArn;
  @JsonProperty(value = "vectors", required = true)
  private List<PutInputVector> vectors;

  public PutVectorRequest(@JsonProperty(value = "vectorBucketName") String vectorBucketName,
      @JsonProperty(value = "indexName") String indexName,
      @JsonProperty(value = "indexArn") String indexArn,
      @JsonProperty(value = "vectors", required = true) List<PutInputVector> vectors) {
    this.vectorBucketName = vectorBucketName;
    this.indexName = indexName;
    this.indexArn = indexArn;
    this.vectors = vectors;
  }

  public String getIndexArn() {
    return indexArn;
  }

  public String getIndexName() {
    return indexName;
  }

  public String getVectorBucketName() {
    return vectorBucketName;
  }

  public List<PutInputVector> getVectors() {
    return vectors;
  }
}
