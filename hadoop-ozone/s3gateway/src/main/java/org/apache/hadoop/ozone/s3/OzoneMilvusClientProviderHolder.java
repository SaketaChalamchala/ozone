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

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import javax.enterprise.inject.Produces;

/**
 * Ozone Milvus Client Provider.
 * <p>
 * As the OzoneConfiguration is created by the CLI application here we inject
 * it via a singleton instance to the Jax-RS/CDI instances.
 */
public class OzoneMilvusClientProviderHolder {

  private static MilvusServiceClient milvusClient = null;

  @Produces
  public MilvusServiceClient milvusClient() {
    return milvusClient;
  }

  public static void initMilvusClient(
      String host, int port) {
    milvusClient = new MilvusServiceClient(
        ConnectParam.newBuilder()
            .withHost(host)
            .withPort(port)
            .build()
    );
  }

  public void close() {
    milvusClient.close();
  }
}
