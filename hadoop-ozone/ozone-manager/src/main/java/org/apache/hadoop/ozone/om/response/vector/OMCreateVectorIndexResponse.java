/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership.  The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.hadoop.ozone.om.response.vector;

import static org.apache.hadoop.ozone.om.codec.OMDBDefinition.BUCKET_TABLE;
import static org.apache.hadoop.ozone.om.codec.OMDBDefinition.VECTOR_INDEX_TABLE;

import java.io.IOException;
import jakarta.annotation.Nonnull;
import org.apache.hadoop.hdds.utils.db.BatchOperation;
import org.apache.hadoop.ozone.om.OMMetadataManager;
import org.apache.hadoop.ozone.om.helpers.OmBucketInfo;
import org.apache.hadoop.ozone.om.helpers.OzoneVectorIndex;
import org.apache.hadoop.ozone.om.response.CleanupTableInfo;
import org.apache.hadoop.ozone.om.response.OMClientResponse;
import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos.OMResponse;

/**
 * Response for Create Vector Index request.
 */
@CleanupTableInfo(cleanupTables = {BUCKET_TABLE, VECTOR_INDEX_TABLE})
public class OMCreateVectorIndexResponse extends OMClientResponse {
  private OmBucketInfo omBucketInfo;
  private OzoneVectorIndex vectorIndex;

  public OMCreateVectorIndexResponse(@Nonnull OMResponse omResponse,
      @Nonnull OmBucketInfo omBucketInfo,
      OzoneVectorIndex ozoneVectorIndex) {
    super(omResponse);
    this.omBucketInfo = omBucketInfo;
    this.vectorIndex = ozoneVectorIndex;
  }

  /**
   * For when the request is not successful.
   * For a successful request, the other constructor should be used.
   */
  public OMCreateVectorIndexResponse(@Nonnull OMResponse omResponse) {
    super(omResponse);
    checkStatusNotOK();

  }

  @Override
  public void addToDBBatch(OMMetadataManager omMetadataManager,
      BatchOperation batchOperation) throws IOException {

    String dbBucketKey =
        omMetadataManager.getBucketKey(omBucketInfo.getVolumeName(),
            omBucketInfo.getBucketName());
    omMetadataManager.getBucketTable().putWithBatch(batchOperation,
        dbBucketKey, omBucketInfo);
    if (vectorIndex != null) {
      omMetadataManager.getVectorIndexTable().putWithBatch(batchOperation, vectorIndex.getIndexName(), vectorIndex);
    }
  }

}
