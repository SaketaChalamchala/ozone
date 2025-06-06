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

package org.apache.hadoop.ozone.om.response.key;

import static org.apache.hadoop.ozone.om.codec.OMDBDefinition.DELETED_TABLE;
import static org.apache.hadoop.ozone.om.codec.OMDBDefinition.SNAPSHOT_INFO_TABLE;
import static org.apache.hadoop.ozone.om.response.snapshot.OMSnapshotMoveDeletedKeysResponse.createRepeatedOmKeyInfo;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import org.apache.hadoop.hdds.utils.db.BatchOperation;
import org.apache.hadoop.hdds.utils.db.DBStore;
import org.apache.hadoop.ozone.om.OMMetadataManager;
import org.apache.hadoop.ozone.om.OmMetadataManagerImpl;
import org.apache.hadoop.ozone.om.OmSnapshot;
import org.apache.hadoop.ozone.om.OmSnapshotManager;
import org.apache.hadoop.ozone.om.helpers.RepeatedOmKeyInfo;
import org.apache.hadoop.ozone.om.helpers.SnapshotInfo;
import org.apache.hadoop.ozone.om.request.key.OMKeyPurgeRequest;
import org.apache.hadoop.ozone.om.response.CleanupTableInfo;
import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos.KeyInfo;
import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos.OMResponse;
import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos.SnapshotMoveKeyInfos;
import org.apache.ratis.util.function.UncheckedAutoCloseableSupplier;

/**
 * Response for {@link OMKeyPurgeRequest} request.
 */
@CleanupTableInfo(cleanupTables = {DELETED_TABLE, SNAPSHOT_INFO_TABLE})
public class OMKeyPurgeResponse extends OmKeyResponse {
  private List<String> purgeKeyList;
  private List<String> renamedList;
  private SnapshotInfo fromSnapshot;
  private List<SnapshotMoveKeyInfos> keysToUpdateList;

  public OMKeyPurgeResponse(@Nonnull OMResponse omResponse,
      @Nonnull List<String> keyList,
      @Nonnull List<String> renamedList,
      SnapshotInfo fromSnapshot,
      List<SnapshotMoveKeyInfos> keysToUpdate) {
    super(omResponse);
    this.purgeKeyList = keyList;
    this.renamedList = renamedList;
    this.fromSnapshot = fromSnapshot;
    this.keysToUpdateList = keysToUpdate;
  }

  /**
   * For when the request is not successful.
   * For a successful request, the other constructor should be used.
   */
  public OMKeyPurgeResponse(@Nonnull OMResponse omResponse) {
    super(omResponse);
    checkStatusNotOK();
  }

  @Override
  public void addToDBBatch(OMMetadataManager omMetadataManager,
      BatchOperation batchOperation) throws IOException {

    if (fromSnapshot != null) {
      OmSnapshotManager omSnapshotManager =
          ((OmMetadataManagerImpl) omMetadataManager).getOzoneManager().getOmSnapshotManager();

      try (UncheckedAutoCloseableSupplier<OmSnapshot> rcOmFromSnapshot =
          omSnapshotManager.getSnapshot(fromSnapshot.getSnapshotId())) {

        OmSnapshot fromOmSnapshot = rcOmFromSnapshot.get();
        DBStore fromSnapshotStore = fromOmSnapshot.getMetadataManager().getStore();
        // Init Batch Operation for snapshot db.
        try (BatchOperation writeBatch =
            fromSnapshotStore.initBatchOperation()) {
          processKeys(writeBatch, fromOmSnapshot.getMetadataManager());
          processKeysToUpdate(writeBatch, fromOmSnapshot.getMetadataManager());
          fromSnapshotStore.commitBatchOperation(writeBatch);
        }
      }
      omMetadataManager.getSnapshotInfoTable().putWithBatch(batchOperation, fromSnapshot.getTableKey(), fromSnapshot);
    } else {
      processKeys(batchOperation, omMetadataManager);
      processKeysToUpdate(batchOperation, omMetadataManager);
    }
  }

  private void processKeysToUpdate(BatchOperation batchOp,
      OMMetadataManager metadataManager) throws IOException {
    if (keysToUpdateList == null) {
      return;
    }

    for (SnapshotMoveKeyInfos keyToUpdate : keysToUpdateList) {
      List<KeyInfo> keyInfosList = keyToUpdate.getKeyInfosList();
      RepeatedOmKeyInfo repeatedOmKeyInfo = createRepeatedOmKeyInfo(keyInfosList);
      metadataManager.getDeletedTable().putWithBatch(batchOp,
          keyToUpdate.getKey(), repeatedOmKeyInfo);
    }
  }

  private void processKeys(BatchOperation batchOp, OMMetadataManager metadataManager) throws IOException {
    for (String key : purgeKeyList) {
      metadataManager.getDeletedTable().deleteWithBatch(batchOp,
          key);
    }
    // Delete rename entries.
    for (String key : renamedList) {
      metadataManager.getSnapshotRenamedTable().deleteWithBatch(batchOp, key);
    }
  }

}
