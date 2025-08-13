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
import java.time.Instant;

public class Index extends IndexSummary {
  @JsonProperty(value = "dataType", required = true)
  private String dataType;
  @JsonProperty(value = "dimension", required = true)
  private short dimension;
  @JsonProperty(value = "distanceMetric", required = true)
  private String distanceMetric;
  @JsonProperty(value = "metadataConfiguration")
  private MetadataConfiguration metadataConfiguration;

  @JsonCreator
  public Index(
      @JsonProperty(value = "creationTime", required = true) Instant creationTime,
      @JsonProperty(value = "dataType", required = true) String dataType,
      @JsonProperty(value = "dimension", required = true) short dimension,
      @JsonProperty(value = "distanceMetric", required = true) String distanceMetric,
      @JsonProperty(value = "indexArn", required = true) String indexArn,
      @JsonProperty(value = "indexName", required = true) String indexName,
      @JsonProperty(value = "vectorBucketName", required = true) String vectorBucketName,
      @JsonProperty(value = "metadataConfiguration") MetadataConfiguration metadataConfiguration) {
    super(creationTime, indexArn, indexName, vectorBucketName);
    this.dataType = dataType;
    this.dimension = dimension;
    this.distanceMetric = distanceMetric;
    this.metadataConfiguration = metadataConfiguration;

  }

  public String getDataType() {
    return dataType;
  }

  public void setDataType(String dataType) {
    this.dataType = dataType;
  }

  public short getDimension() {
    return dimension;
  }

  public void setDimension(short dimension) {
    this.dimension = dimension;
  }

  public String getDistanceMetric() {
    return distanceMetric;
  }

  public void setDistanceMetric(String distanceMetric) {
    this.distanceMetric = distanceMetric;
  }

  public MetadataConfiguration getMetadataConfiguration() {
    return metadataConfiguration;
  }

  public void setMetadataConfiguration(
      MetadataConfiguration metadataConfiguration) {
    this.metadataConfiguration = metadataConfiguration;
  }
}
