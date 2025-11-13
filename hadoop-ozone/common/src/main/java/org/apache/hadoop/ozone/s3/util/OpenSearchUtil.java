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

package org.apache.hadoop.ozone.s3.util;

import java.util.HashMap;
import java.util.Map;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.indices.IndexSettings;

public class OpenSearchUtil {

  public static final String KNN_VECTOR_METHOD = "hnsw";
  public static final String KNN_VECTOR_ENGINE = "lucene";
  public static final Map<String, JsonData> KNN_VECTOR_METHOD_PARAMS = createKKNmethosdParams();
  public static final IndexSettings INDEX_SETTINGS = new IndexSettings.Builder().knn(true).build();

  public static final String INDEX_NAME_FIELD = "indexName";
  public static final String VECTOR_FIELD = "embedding";
  public static final String METADATA_FIELD = "metadata";

  public static Map<String, JsonData> createKKNmethosdParams() {
    Map<String, JsonData> params = new HashMap<>();
    params.put("ef_construction", JsonData.of(200));
    params.put("m", JsonData.of(32));
    return params;
  }

  public static String getOpenSearchSpaceType(String distanceMetric) {
    switch (distanceMetric) {
    case "cosine":
      return "cosinesimil";
    case "euclidean":
      return "l2";
    default:
      throw new UnsupportedOperationException("Unsupported distance metric: " + distanceMetric);
    }
  }
}
