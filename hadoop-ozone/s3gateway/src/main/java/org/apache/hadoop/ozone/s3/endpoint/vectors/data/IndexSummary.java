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
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class IndexSummary {
  @JsonProperty(value = "creationTime", required = true)
  @JsonFormat(shape = JsonFormat.Shape.STRING)
  private Instant creationTime;
  @JsonProperty(value = "indexArn", required = true)
  private String indexArn;
  @JsonProperty(value = "indexName", required = true)
  private String indexName;
  @JsonProperty(value = "vectorBucketName", required = true)
  private String vectorBucketName;

  @JsonCreator
  public IndexSummary(@JsonProperty(value = "creationTime", required = true) Instant creationTime,
      @JsonProperty(value = "indexArn", required = true) String indexArn,
      @JsonProperty(value = "indexName", required = true) String indexName,
      @JsonProperty(value = "vectorBucketName", required = true) String vectorBucketName) {
    this.creationTime = creationTime;
    this.indexArn = indexArn;
    this.indexName = indexName;
    this.vectorBucketName = vectorBucketName;
  }

  public Instant getCreationTime() {
    return creationTime;
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
}
