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

import static org.apache.hadoop.ozone.s3.util.OpenSearchUtil.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.hadoop.hdds.conf.ConfigurationSource;
import org.apache.hadoop.hdds.server.JsonUtils;
import org.apache.hadoop.ozone.OzoneConfigKeys;
import org.apache.hadoop.ozone.om.helpers.OzoneVectorIndex;
import org.apache.hadoop.ozone.s3.endpoint.vectors.data.IndexData;
import org.apache.hadoop.ozone.s3.endpoint.vectors.data.QueryOutputVector;
import org.apache.hadoop.ozone.s3.endpoint.vectors.data.VectorData;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.KnnVectorProperty;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch._types.query_dsl.KnnQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpensearchVectorStore implements VectorStore {
  private static final Logger LOG = LoggerFactory.getLogger(OpensearchVectorStore.class);
  private final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
  private final HttpHost host;
  private final RestClient restClient;
  private final OpenSearchTransport transport;
  private final OpenSearchClient client;

  public OpensearchVectorStore(ConfigurationSource conf) throws URISyntaxException {
    credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(
        "admin", "admin"));
    this.host = new HttpHost(conf.get(OzoneConfigKeys.OZONE_S3G_VECTOR_HOST),
        conf.getInt(OzoneConfigKeys.OZONE_S3G_VECTOR_PORT, 0), "http");
    this.restClient = RestClient.builder(this.host).setHttpClientConfigCallback(
        new RestClientBuilder.HttpClientConfigCallback() {
          @Override
          public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
            return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
          }
        }).build();
    this.transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
    this.client = new OpenSearchClient(transport);
  }

  @Override
  public void createIndex(List<OzoneVectorIndex> vectorIndices) throws IOException {
    for (OzoneVectorIndex vectorIndex : vectorIndices) {
      String schemaName = vectorIndex.getIndexName();
      try {
        KnnVectorProperty knnVectorProperty = new KnnVectorProperty.Builder()
            .dataType(vectorIndex.getDataType())
            .dimension(vectorIndex.getDimension())
            .method(m -> m
                .name(KNN_VECTOR_METHOD)
                .engine(KNN_VECTOR_ENGINE)
                .spaceType(getOpenSearchSpaceType(vectorIndex.getDistanceMetric()))
                .parameters(KNN_VECTOR_METHOD_PARAMS))
            .build();
        Property vectorProperty = new Property.Builder()
            .knnVector(knnVectorProperty)
            .build();
        Map<String, Property> propertiesMap = new HashMap<>();
        propertiesMap.put(INDEX_NAME_FIELD, new Property.Builder().keyword(k -> k).build());
        propertiesMap.put(VECTOR_FIELD, vectorProperty);
        propertiesMap.put(METADATA_FIELD, new Property.Builder().keyword(k -> k).build());
        TypeMapping mapping = new TypeMapping.Builder()
            .properties(propertiesMap)
            .build();

        CreateIndexRequest createRequest = new CreateIndexRequest.Builder()
            .index(schemaName)
            .settings(INDEX_SETTINGS)
            .mappings(mapping)
            .build();

        CreateIndexResponse createResponse = client.indices().create(createRequest);

        if (createResponse != null && createResponse.acknowledged()) {
          LOG.info("Index created successfully: {}", schemaName);
        }
      } catch (IOException e) {
        LOG.error("Error creating index: {}",  schemaName, e);
        throw e;
      }
    }
  }

  private static String bucketIndexName(String bucket, String index) {
    return bucket + "/" + index;
  }

  @Override
  public void putVectorData(String bucketName, String indexName, String schemaName, String key, JsonNode node,
      VectorData data) throws IOException {
    IndexData indexData = new IndexData(bucketIndexName(bucketName, indexName),
        data.getFloat32(), JsonUtils.toJsonString(node));
    try {
      IndexRequest<IndexData> indexRequest = new IndexRequest.Builder<IndexData>()
          .index(schemaName)
          .id(key)
          .document(indexData)
          .build();

      IndexResponse indexResponse = client.index(indexRequest);

      if (indexResponse != null) {
        LOG.info("Document indexed successfully result: {}", indexResponse.result().jsonValue());
      }
    } catch (IOException e) {
      LOG.error("Error indexing document with key: {}", key, e);
      throw e;
    }
  }

  public static List<QueryOutputVector> extractResults(SearchResponse<IndexData> response) throws IOException {
    List<QueryOutputVector> results = new ArrayList<>();
    LOG.info("Query output:");
    for (Hit<IndexData> hit : response.hits().hits()) {
      List<Float> embedding = hit.source().getEmbedding();
      float distance = hit.score().floatValue();
      String metadataStr = hit.source().getMetadata();
      JsonNode metadata = metadataStr != null ? JsonUtils.fromJson(metadataStr, JsonNode.class) : null;
      String key = hit.id();

      results.add(new QueryOutputVector(new VectorData(embedding), distance, key, metadata));

      LOG.info("---------------------------------");
      LOG.info("ID: " + key);
      LOG.info("Distance: " + distance);
      LOG.info(INDEX_NAME_FIELD + ": {}", hit.source().getIndexName());
      LOG.info("Vector size: {}", embedding.size());
      LOG.info(METADATA_FIELD + ": {}", metadataStr);
    }
    return results;
  }

  @Override
  public List<QueryOutputVector> getVectorData(String bucketName, String indexName, String schemaName,
      VectorData queryVectorData, int numberOfEntries) throws Exception {
    Query filterQuery = new Query.Builder()
        .term(t -> t
            .field(INDEX_NAME_FIELD)
            .value(v -> v.stringValue(bucketIndexName(bucketName, indexName)))
        )
        .build();
    KnnQuery knnQuery = new KnnQuery.Builder()
        .filter(filterQuery)
        .field(VECTOR_FIELD)
        .vector(ArrayUtils.toPrimitive(queryVectorData.getFloat32().toArray(new Float[0]), 0))
        .k(numberOfEntries)
        .build();

    SearchRequest searchRequest = new SearchRequest.Builder()
        .index(schemaName)
        .query(new Query.Builder().knn(knnQuery).build())
        .build();

    try {
      SearchResponse<IndexData> response = client.search(searchRequest, IndexData.class);
      return extractResults(response);
    } catch (IOException e) {
      LOG.error("Error during k-NN search: ", e);
      throw e;
    }
  }

  @Override
  public void close() throws Exception {
    transport.close();
  }
}
