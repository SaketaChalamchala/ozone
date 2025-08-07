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

package org.apache.hadoop.ozone.s3.endpoint.vectors.store;

import static org.apache.hadoop.ozone.s3.util.VespaUtil.deploySchema;

import ai.vespa.feed.client.DocumentId;
import ai.vespa.feed.client.FeedClient;
import ai.vespa.feed.client.FeedClientBuilder;
import ai.vespa.feed.client.OperationParameters;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.hdds.conf.ConfigurationSource;
import org.apache.hadoop.hdds.server.JsonUtils;
import org.apache.hadoop.hdds.utils.LegacyHadoopConfigurationSource;
import org.apache.hadoop.hdfs.web.URLConnectionFactory;
import org.apache.hadoop.ozone.s3.endpoint.vectors.data.ListOutputVector;
import org.apache.hadoop.ozone.s3.endpoint.vectors.data.VectorData;
import org.apache.hadoop.ozone.s3.util.VespaUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VespaVectorStore implements VectorStore {
  private static final Logger LOG = LoggerFactory.getLogger(VespaVectorStore.class);
  private final String host;
  private final int port;
  private final FeedClient client;
  private final URLConnectionFactory factory;

  public VespaVectorStore(String host, int port, ConfigurationSource conf) throws URISyntaxException {
    this.host = host;
    this.port = port;
    client = FeedClientBuilder.create(new URI(host + ":" + port))
        .build();
    factory = URLConnectionFactory
        .newDefaultURLConnectionFactory(5000, 2000,
            LegacyHadoopConfigurationSource.asHadoopConfiguration(conf));
    ;
  }

  @Override
  public String createIndex(String bucketName, String indexName, String distanceMetric, int dimension)
      throws IOException {
    return deploySchema(dimension, host, port, distanceMetric);
  }

  private static String bucketIndexName(String bucket, String index) {
    return bucket + "/" + index;
  }

  @Override
  public void putVectorData(String bucketName, String indexName, String schemaName, String key, JsonNode node,
      VectorData data) throws IOException {
    Map<String, Object> embeddingMap = new HashMap<>();
    embeddingMap.put("values", data.getFloat32());

    Map<String, Object> fields = new LinkedHashMap<>();
    String bucketIndexName = bucketIndexName(bucketName, indexName);
    fields.put("indexName", bucketIndexName);
    fields.put("embedding", embeddingMap);
    fields.put("metadata", node);

    Map<String, Object> root = new HashMap<>();
    root.put("fields", fields);

    String jsonDoc = JsonUtils.toJsonString(root);
    DocumentId docApiId = DocumentId.of("id:" + schemaName + ":" + schemaName + "::" + key);
    OperationParameters params = OperationParameters.empty()
        .timeout(Duration.ofSeconds(5));
    client.put(docApiId, jsonDoc, params);
  }

  @Override
  public List<ListOutputVector> getVectorData(String bucketName, String indexName, VectorData data,
      int numberOfEntries) throws Exception {
    String bucketIndexName = bucketIndexName(bucketName, indexName);
    String query = VespaUtil.buildNearestNeighborQuery(data.getFloat32(), numberOfEntries, bucketIndexName);
    JsonNode output = JsonUtils.fromJson(VespaUtil.postJsonWithURLFactory(factory, String.format("%s:%d/search/", host,
        port), query), JsonNode.class);
    LOG.info("Query output: {}", output);
    return Collections.emptyList();
  }

  @Override
  public void close() throws Exception {
    client.close();
  }
}
