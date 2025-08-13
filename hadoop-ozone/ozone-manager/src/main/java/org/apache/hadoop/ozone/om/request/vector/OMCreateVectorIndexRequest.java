/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.ozone.om.request.vector;

import static org.apache.hadoop.ozone.om.lock.OzoneManagerLock.LeveledResource.BUCKET_LOCK;
import static org.apache.hadoop.ozone.s3.util.VectorUtil.getVectorBucketSchemaMetadata;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.hadoop.hdds.utils.db.Table;
import org.apache.hadoop.hdds.utils.db.TableIterator;
import org.apache.hadoop.hdds.utils.db.cache.CacheKey;
import org.apache.hadoop.hdds.utils.db.cache.CacheValue;
import org.apache.hadoop.ozone.audit.AuditLogger;
import org.apache.hadoop.ozone.audit.OMAction;
import org.apache.hadoop.ozone.om.IOmMetadataReader;
import org.apache.hadoop.ozone.om.OMMetadataManager;
import org.apache.hadoop.ozone.om.OMMetrics;
import org.apache.hadoop.ozone.om.OmMetadataReader;
import org.apache.hadoop.ozone.om.OzoneManager;
import org.apache.hadoop.ozone.om.execution.flowcontrol.ExecutionContext;
import org.apache.hadoop.ozone.om.exceptions.OMException;
import org.apache.hadoop.ozone.om.helpers.OmBucketInfo;
import org.apache.hadoop.ozone.om.helpers.OzoneVectorIndex;
import org.apache.hadoop.ozone.om.request.OMClientRequest;
import org.apache.hadoop.ozone.om.request.util.OmResponseUtil;
import org.apache.hadoop.ozone.om.response.OMClientResponse;
import org.apache.hadoop.ozone.om.response.bucket.OMBucketSetPropertyResponse;
import org.apache.hadoop.ozone.om.response.vector.OMCreateVectorIndexResponse;
import org.apache.ratis.util.function.UncheckedAutoCloseableSupplier;
import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos;
import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos.OMRequest;
import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos.OMResponse;
import org.apache.hadoop.ozone.security.acl.IAccessAuthorizer;
import org.apache.hadoop.ozone.security.acl.OzoneObj;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.util.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle SetBucketProperty Request.
 */
public class OMCreateVectorIndexRequest extends OMClientRequest {
  private static final Logger LOG =
      LoggerFactory.getLogger(OMCreateVectorIndexRequest.class);
  private static final Lock lock = new ReentrantLock();

  public OMCreateVectorIndexRequest(OMRequest omRequest) {
    super(omRequest);
  }

  @Override
  public OMRequest preExecute(OzoneManager ozoneManager)
      throws IOException {
    long modificationTime = Time.now();
    OzoneManagerProtocolProtos.CreateVectorIndexRequest.Builder
        createVectorIndexRequestBuilder = super.preExecute(ozoneManager)
        .getCreateVectorIndex().toBuilder()
        .setModificationTime(modificationTime);
    // check Acl
    if (ozoneManager.getAclsEnabled()) {
      checkAclPermission(ozoneManager, createVectorIndexRequestBuilder.getVolume(),
          createVectorIndexRequestBuilder.getBucket());
    }
    return getOmRequest().toBuilder()
        .setCreateVectorIndex(createVectorIndexRequestBuilder)
        .setUserInfo(getUserInfo())
        .build();
  }

  @Override
  @SuppressWarnings("methodlength")
  public OMClientResponse validateAndUpdateCache(OzoneManager ozoneManager, ExecutionContext context) {
    final long transactionLogIndex = context.getIndex();

    OzoneManagerProtocolProtos.CreateVectorIndexRequest vectorCreateRequest =
        getOmRequest().getCreateVectorIndex();
    Preconditions.checkNotNull(vectorCreateRequest);

    OMMetadataManager omMetadataManager = ozoneManager.getMetadataManager();
    OMMetrics omMetrics = ozoneManager.getMetrics();
    omMetrics.incNumBucketUpdates();

    String volumeName = vectorCreateRequest.getVolume();
    String bucketName = vectorCreateRequest.getBucket();
    String indexName = vectorCreateRequest.getIndexName();
    OzoneVectorIndex vectorIndex = OzoneVectorIndex.getFromProtobuf(vectorCreateRequest.getVectorIndex());
    OMResponse.Builder omResponse = OmResponseUtil.getOMResponseBuilder(
        getOmRequest());

    OmBucketInfo omBucketInfo = null;

    AuditLogger auditLogger = ozoneManager.getAuditLogger();
    OzoneManagerProtocolProtos.UserInfo userInfo = getOmRequest().getUserInfo();
    Exception exception = null;
    boolean acquiredBucketLock = false, success = true;
    OMClientResponse omClientResponse = null;
    try {
      if (!omMetadataManager.getVectorIndexTable().isExist(vectorIndex.getIndexName())) {
        lock.lock();
        try (TableIterator<String, ? extends Table.KeyValue<String, OzoneVectorIndex>> itr =
                 omMetadataManager.getVectorIndexTable().iterator()) {
          if (!omMetadataManager.getVectorIndexTable().isExist(vectorIndex.getIndexName())) {
            LOG.info("Creating vector index: {} ", vectorIndex.getIndexName());
            List<OzoneVectorIndex> vectorIndices = Lists.newArrayList();
            while (itr.hasNext()) {
              vectorIndices.add(itr.next().getValue());
            }
            vectorIndices.add(vectorIndex);
            if (ozoneManager.isLeaderReady()) {
              ozoneManager.getVectorStore().createIndex(vectorIndices);
            }
            omMetadataManager.getVectorIndexTable().addCacheEntry(
                new CacheKey<>(vectorIndex.getIndexName()),
                CacheValue.get(transactionLogIndex, vectorIndex));
          }
        } finally {
          lock.unlock();
        }
      }

      // acquire lock.
      mergeOmLockDetails(
          omMetadataManager.getLock().acquireWriteLock(BUCKET_LOCK, volumeName, bucketName));
      acquiredBucketLock = getOmLockDetails().isLockAcquired();

      String bucketKey = omMetadataManager.getBucketKey(volumeName, bucketName);
      OmBucketInfo dbBucketInfo =
          omMetadataManager.getBucketTable().get(bucketKey);
      //Check if bucket exist
      if (dbBucketInfo == null) {
        LOG.debug("bucket: {} not found ", bucketName);
        throw new OMException("Bucket doesn't exist",
            OMException.ResultCodes.BUCKET_NOT_FOUND);
      }

      if (dbBucketInfo.isLink()) {
        throw new OMException("Cannot set property on link",
            OMException.ResultCodes.NOT_SUPPORTED_OPERATION);
      }

      OmBucketInfo.Builder bucketInfoBuilder = dbBucketInfo.toBuilder();
      bucketInfoBuilder.setUpdateID(transactionLogIndex);
      String metadataKey = getVectorBucketSchemaMetadata(indexName);
      bucketInfoBuilder.addMetadata(metadataKey, vectorIndex.getIndexName());
      bucketInfoBuilder.setModificationTime(vectorCreateRequest.getModificationTime());

      omBucketInfo = bucketInfoBuilder.build();

      // Update table cache.
      omMetadataManager.getBucketTable().addCacheEntry(
          new CacheKey<>(bucketKey),
          CacheValue.get(transactionLogIndex, omBucketInfo));

      omResponse.setCreateVectorIndexResponse(
          OzoneManagerProtocolProtos.CreateVectorIndexResponse.newBuilder()
              .setIndexName(vectorIndex.getIndexName()).build());
      omClientResponse = new OMCreateVectorIndexResponse(
          omResponse.build(), omBucketInfo, vectorIndex);
    } catch (IOException | InvalidPathException ex) {
      success = false;
      exception = ex;
      omClientResponse = new OMBucketSetPropertyResponse(
          createErrorOMResponse(omResponse, exception));
    } finally {
      if (acquiredBucketLock) {
        mergeOmLockDetails(
            omMetadataManager.getLock().releaseWriteLock(BUCKET_LOCK, volumeName, bucketName));
      }
    }

    // Performing audit logging outside of the lock.
    markForAudit(auditLogger, buildAuditMessage(OMAction.CREATE_VECTOR_INDEX,
        Maps.newHashMap(ImmutableMap.of("volume", volumeName, "bucket", bucketName, "indexName", indexName)),
        exception, userInfo));

    // return response.
    if (success) {
      LOG.debug("Setting bucket property for bucket:{} in volume:{}",
          bucketName, volumeName);
      return omClientResponse;
    } else {
      LOG.error("Setting bucket property failed for bucket:{} in volume:{}",
          bucketName, volumeName, exception);
      omMetrics.incNumBucketUpdateFails();
      return omClientResponse;
    }
  }

  private void checkAclPermission(
      OzoneManager ozoneManager, String volumeName, String bucketName)
      throws IOException {
    final boolean nativeAuthorizerEnabled;
    try (UncheckedAutoCloseableSupplier<IOmMetadataReader> rcMetadataReader =
        ozoneManager.getOmMetadataReader()) {
      OmMetadataReader mdReader = (OmMetadataReader) rcMetadataReader.get();
      nativeAuthorizerEnabled = mdReader.isNativeAuthorizerEnabled();
    }
    if (nativeAuthorizerEnabled) {
      UserGroupInformation ugi = createUGIForApi();
      String bucketOwner = ozoneManager.getBucketOwner(volumeName, bucketName,
          IAccessAuthorizer.ACLType.READ, OzoneObj.ResourceType.BUCKET);
      if (!ozoneManager.isAdmin(ugi) &&
          !ozoneManager.isOwner(ugi, bucketOwner)) {
        throw new OMException(
            "Bucket properties are allowed to changed by Admin and Owner",
            OMException.ResultCodes.PERMISSION_DENIED);
      }
    } else { // ranger acl
      checkAcls(ozoneManager, OzoneObj.ResourceType.BUCKET,
          OzoneObj.StoreType.OZONE, IAccessAuthorizer.ACLType.WRITE,
          volumeName, bucketName, null);
    }
  }
}
