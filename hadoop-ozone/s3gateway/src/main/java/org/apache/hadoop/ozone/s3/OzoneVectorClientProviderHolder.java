/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.ozone.s3;

import java.net.URISyntaxException;
import javax.enterprise.inject.Produces;
import org.apache.hadoop.hdds.conf.ConfigurationSource;
import org.apache.hadoop.ozone.OzoneConsts.VectorDB;
import org.apache.hadoop.ozone.s3.endpoint.vectors.store.OpensearchVectorStore;
import org.apache.hadoop.ozone.s3.endpoint.vectors.store.VectorStore;
import org.apache.hadoop.ozone.s3.endpoint.vectors.store.VespaVectorStore;

import static org.apache.hadoop.ozone.OzoneConfigKeys.OZONE_S3G_VECTOR_DB;
import static org.apache.hadoop.ozone.OzoneConfigKeys.OZONE_S3G_VECTOR_DB_DEFAULT;

/**
 * Ozone Milvus Client Provider.
 * <p>
 * As the OzoneConfiguration is created by the CLI application here we inject
 * it via a singleton instance to the Jax-RS/CDI instances.
 */
public class OzoneVectorClientProviderHolder {

  private static VectorStore vectorStore = null;

  @Produces
  public VectorStore vectorStore() {
    return vectorStore;
  }

  private static VectorStore initVespaVectorStore(ConfigurationSource conf)
      throws URISyntaxException {
    return new VespaVectorStore(conf);
  }

  private static VectorStore initOpensearchVectorStore(ConfigurationSource conf)
      throws URISyntaxException {
    return new OpensearchVectorStore(conf);
  }

  public static void initVectorStore(ConfigurationSource conf) throws URISyntaxException {
    switch (VectorDB.valueOf(conf.get(OZONE_S3G_VECTOR_DB, OZONE_S3G_VECTOR_DB_DEFAULT))) {
    case VESPA:
      vectorStore = initVespaVectorStore(conf);
      break;
    case OPENSEARCH:
      vectorStore = initOpensearchVectorStore(conf);
      break;
    default:
      throw new IllegalArgumentException("Unsupported vector DB");
    }
  }

  public static void close() throws Exception {
    vectorStore.close();
  }
}
