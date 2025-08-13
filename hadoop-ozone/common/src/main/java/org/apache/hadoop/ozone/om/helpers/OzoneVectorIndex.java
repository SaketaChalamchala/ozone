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

package org.apache.hadoop.ozone.om.helpers;

import org.apache.hadoop.hdds.utils.db.Codec;
import org.apache.hadoop.hdds.utils.db.CopyObject;
import org.apache.hadoop.hdds.utils.db.DelegatedCodec;
import org.apache.hadoop.hdds.utils.db.Proto2Codec;
import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos.VectorIndex;

public class OzoneVectorIndex implements CopyObject<OzoneVectorIndex> {

  private final String dataType;
  private final int dimension;
  private final String distanceMetric;

  private OzoneVectorIndex(String dataType, int dimension, String distanceMetric) {
    this.dataType = dataType;
    this.dimension = dimension;
    this.distanceMetric = distanceMetric;
  }

  public String getDataType() {
    return dataType;
  }

  public int getDimension() {
    return dimension;
  }

  public String getDistanceMetric() {
    return distanceMetric;
  }

  public String getIndexName() {
    return dataType + "_" + dimension + "_" + distanceMetric;
  }

  private static final Codec<OzoneVectorIndex> CODEC = new DelegatedCodec<>(
      Proto2Codec.getFromClass(VectorIndex.class),
      OzoneVectorIndex::getFromProtobuf,
      OzoneVectorIndex::getProtobuf,
      OzoneVectorIndex.class);

  public static Codec<OzoneVectorIndex> getCodec() {
    return CODEC;
  }

  public static OzoneVectorIndex getFromProtobuf(VectorIndex vectorIndex) {
    return new Builder()
        .setDataType(vectorIndex.getDatatype())
        .setDimension(vectorIndex.getDimension())
        .setDistanceMetric(vectorIndex.getDistanceMetric())
        .build();
  }

  public static VectorIndex getProtobuf(OzoneVectorIndex vectorIndex) {
    return VectorIndex.newBuilder()
        .setDatatype(vectorIndex.getDataType())
        .setDimension(vectorIndex.getDimension())
        .setDistanceMetric(vectorIndex.getDistanceMetric())
        .build();
  }

  @Override
  public OzoneVectorIndex copyObject() {
    return newBuilder().build();
  }

  public Builder newBuilder() {
    return new Builder()
        .setDataType(dataType)
        .setDimension(dimension)
        .setDataType(dataType);
  }

  public static class Builder {
    private String dataType;
    private int dimension;
    private String distanceMetric;
    private String indexName;

    public Builder() {
    }

    public Builder setDataType(String dataType) {
      this.dataType = dataType;
      return this;
    }

    public Builder setDimension(int dimension) {
      this.dimension = dimension;
      return this;
    }

    public Builder setDistanceMetric(String distanceMetric) {
      this.distanceMetric = distanceMetric;
      return this;
    }

    public OzoneVectorIndex build() {
      return new OzoneVectorIndex(dataType, dimension, distanceMetric);
    }
  }
}
