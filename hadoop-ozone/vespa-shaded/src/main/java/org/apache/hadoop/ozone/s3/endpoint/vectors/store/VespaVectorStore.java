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

import static org.apache.hadoop.ozone.OzoneConfigKeys.OZONE_VECTOR_HOST_MAP;
import static org.apache.hadoop.ozone.s3.util.VespaUtil.deploySchema;

import ai.vespa.feed.client.DocumentId;
import ai.vespa.feed.client.FeedClient;
import ai.vespa.feed.client.FeedClientBuilder;
import ai.vespa.feed.client.OperationParameters;
import ai.vespa.feed.client.Result;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.hadoop.hdds.conf.ConfigurationSource;
import org.apache.hadoop.hdds.server.JsonUtils;
import org.apache.hadoop.hdds.utils.LegacyHadoopConfigurationSource;
import org.apache.hadoop.hdfs.web.URLConnectionFactory;
import org.apache.hadoop.ozone.OzoneConfigKeys;
import org.apache.hadoop.ozone.om.helpers.OzoneVectorIndex;
import org.apache.hadoop.ozone.s3.endpoint.vectors.data.ListOutputVector;
import org.apache.hadoop.ozone.s3.endpoint.vectors.data.QueryOutputVector;
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
  private final int schemaPort;
  private final String schemaHost;
  private final Map<String, String> hostMap;

  public VespaVectorStore(ConfigurationSource conf) throws URISyntaxException {
    this.host = conf.get(OzoneConfigKeys.OZONE_S3G_VECTOR_HOST);
    this.port = conf.getInt(OzoneConfigKeys.OZONE_S3G_VECTOR_PORT, 0);
    this.schemaHost = conf.get(OzoneConfigKeys.OZONE_S3G_SCHEMA_VECTOR_HOST);
    this.schemaPort = conf.getInt(OzoneConfigKeys.OZONE_S3G_SCHEMA_VECTOR_PORT, 0);
    client = FeedClientBuilder.create(new URI(host + ":" + port))
        .build();
    factory = URLConnectionFactory
        .newDefaultURLConnectionFactory(5000, 2000,
            LegacyHadoopConfigurationSource.asHadoopConfiguration(conf));
    hostMap = Arrays.stream(conf.get(OZONE_VECTOR_HOST_MAP).split(";"))
        .collect(Collectors.toMap(i -> i.split("=")[0], i -> i.split("=")[1]));
  }

  @Override
  public void createIndex(List<OzoneVectorIndex> vectorIndices)
      throws IOException {
    VespaUtil.deploySchema(factory, schemaHost, schemaPort, vectorIndices, hostMap);
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
    fields.put("metadata", JsonUtils.toJsonString(node));

    Map<String, Object> root = new HashMap<>();
    root.put("fields", fields);

    String jsonDoc = JsonUtils.toJsonString(root);
    DocumentId docApiId = DocumentId.of("id:" + schemaName + ":" + schemaName +
        "::" + bucketIndexName + "_" + key);
    OperationParameters params = OperationParameters.empty()
        .timeout(Duration.ofSeconds(5));
    try {
      Result res = client.put(docApiId, jsonDoc, params).get();
      LOG.debug("Put document result: {}", res);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOG.error("Interrupted while waiting for response from Vespa", e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public static List<QueryOutputVector> extractResults(JsonNode root, String bucketIndexName) throws IOException {
    List<QueryOutputVector> results = new ArrayList<>();

    JsonNode children = root.path("root").path("children");
    for (JsonNode child : children) {

      // Extract embedding values
      JsonNode embeddingValues = child.path("fields").path("embedding").path("values");
      List<Float> embeddingList = new ArrayList<>();
      for (JsonNode valueNode : embeddingValues) {
        embeddingList.add(valueNode.floatValue());
      }

      // Extract distance
      JsonNode distanceNode = child.path("fields").path("matchfeatures").path("distance(field,embedding)");
      float distance = distanceNode.floatValue();

      // Extract metadata string
      JsonNode metadata = JsonUtils.fromJson(child.path("fields").path("metadata").asText(), JsonNode.class);

      // Extract key from document id
      String docId = child.path("id").asText();
      String key = docId;
      int lastIndex = docId.lastIndexOf("::");
      if (lastIndex != -1 && lastIndex + 2 < docId.length()) {
        key = docId.substring(lastIndex + 2).replace(bucketIndexName + "_", "");
      }

      results.add(new QueryOutputVector(new VectorData(embeddingList), distance, key, metadata));
    }
    return results;
  }

  @Override
  public List<QueryOutputVector> getVectorData(String bucketName, String indexName, String schemaName, VectorData data,
      int numberOfEntries) throws Exception {
    String bucketIndexName = bucketIndexName(bucketName, indexName);
    String query = VespaUtil.buildNearestNeighborQuery(data.getFloat32(), numberOfEntries, schemaName, bucketIndexName);
    JsonNode output = JsonUtils.fromJson(VespaUtil.postJsonWithURLFactory(factory, String.format("%s:%d/search/", host,
        port), query), JsonNode.class);
    LOG.info("Query output: {}", output);
    return extractResults(output, bucketIndexName);
  }

  @Override
  public void close() throws Exception {
    client.close();
    factory.destroy();
  }
}
