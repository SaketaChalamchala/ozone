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

public class VectorBucket extends VectorBucketSummary {
  @JsonProperty(value = "encryptionConfiguration")
  private EncryptionConfiguration encryptionConfiguration;

  @JsonCreator
  public VectorBucket(@JsonProperty(value = "creationTime", required = true) Instant creationTime,
      @JsonProperty(value = "vectorBucketArn", required = true) String vectorBucketArn,
      @JsonProperty(value = "vectorBucketName", required = true) String vectorBucketName,
      @JsonProperty(value = "encryptionConfiguration") EncryptionConfiguration encryptionConfiguration) {
    super(creationTime, vectorBucketArn, vectorBucketName);
    this.encryptionConfiguration = encryptionConfiguration;
  }

  public EncryptionConfiguration getEncryptionConfiguration() {
    return encryptionConfiguration;
  }
}
