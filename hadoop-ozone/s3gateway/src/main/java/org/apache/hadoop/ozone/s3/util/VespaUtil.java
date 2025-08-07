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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.hadoop.hdds.server.JsonUtils;
import org.apache.hadoop.hdfs.web.URLConnectionFactory;

public class VespaUtil {

  public static String getSchemaName(int vectorSize, String distanceMetric) {
    return "vector_schema_" + vectorSize + "_" + distanceMetric;
  }

  public static String deploySchema(int vectorSize, String host, int port, String distanceMetric) throws IOException {
    String schemaName = getSchemaName(vectorSize, distanceMetric);
    String vespaHost = host + ":" + port;
    Path tempDir = Files.createTempDirectory("vespa_app");
    Files.createDirectories(tempDir.resolve("search"));
    String servicesXml = "<services version=\"1.0\">\n" +
            "  <container id=\"default\" version=\"1.0\">\n" +
            "    <search/>\n" +
            "    <document-api/>\n" +
            "    <nodes count=\"1\"/>\n" +
            "  </container>\n" +
            "</services>\n";
    Files.write(tempDir.resolve("services.xml"), servicesXml.getBytes(StandardCharsets.UTF_8));

    // deployment.xml
    String deploymentXml = "<deployment version='1.0'></deployment>";
    Files.write(tempDir.resolve("deployment.xml"), deploymentXml.getBytes(StandardCharsets.UTF_8));

    // schema (.sd) with vector, indexName, and non-searchable JSON string
    String schemaContent =
        "schema " + schemaName + " {\n" +
            "  document " + schemaName + " {\n" +
            "    field indexName type string {\n" +
            "      indexing: summary | attribute\n" +
            "    }\n" +
            "    field embedding type tensor<float>(x[" + vectorSize + "]) {\n" +
            "      indexing: summary | attribute\n" +
            "      attribute {\n" +
            "        distance-metric: " + distanceMetric + "\n" +
            "        index: hnsw\n" +
            "      }\n" +
            "    }\n" +
            "    field metadata type string {\n" +
            "      indexing: summary\n" +
            "      indexing: input metadata | summary\n" +
            "      rank: filter\n" +
            "    }\n" +
            "  }\n" +
            "  fieldset default {\n" +
            "    fields: indexName\n" +
            "  }\n" +
            "}\n";
    Files.write(tempDir.resolve("search").resolve(schemaName + ".sd"),
        schemaContent.getBytes(StandardCharsets.UTF_8));
    Path appZip = Files.createTempFile("vespa_app", ".zip");
    zipDirectory(tempDir, appZip);
    String sessionId = createSession(appZip.toFile(), vespaHost);
    prepareSession(vespaHost, sessionId);
    activateSession(vespaHost, sessionId);
    return schemaName;
  }

  private static String createSession(File zipFile, String vespaHost) throws IOException {
    URL url = new URL(vespaHost + "/application/v2/tenant/default/session");
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setDoOutput(true);
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/zip");

    try (OutputStream os = conn.getOutputStream()) {
      Files.copy(zipFile.toPath(), os);
    }

    String response = readResponse(conn);
    System.out.println("Create Session Response: " + response);
    return response.replaceAll("\\D+", "");
  }

  private static void prepareSession(String vespaHost, String sessionId) throws IOException {
    URL url = new URL(vespaHost + "/application/v2/tenant/default/session/" + sessionId + "/prepare");
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.getResponseCode();
  }

  private static void activateSession(String vespaHost, String sessionId) throws IOException {
    URL url = new URL(vespaHost + "/application/v2/tenant/default/session/" + sessionId + "/activate");
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.getResponseCode();
  }

  private static String readResponse(HttpURLConnection conn) throws IOException {
    try (InputStream is = conn.getInputStream();
         BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line);
      }
      return sb.toString();
    }
  }

  private static void zipDirectory(Path sourceDir, Path zipFile) throws IOException {
    try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(zipFile))) {
      Files.walk(sourceDir).filter(path -> !Files.isDirectory(path)).forEach(path -> {
        ZipEntry zipEntry = new ZipEntry(sourceDir.relativize(path).toString());
        try {
          zs.putNextEntry(zipEntry);
          Files.copy(path, zs);
          zs.closeEntry();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    }
  }

  public static String postJsonWithURLFactory(URLConnectionFactory factory, String urlString,
      String jsonBody) throws Exception {
    URL url = new URL(urlString);
    HttpURLConnection conn = (HttpURLConnection) factory.openConnection(url);
    conn.setRequestMethod("POST");
    conn.setDoOutput(true);
    conn.setRequestProperty("Content-Type", "application/json");

    try (OutputStream os = conn.getOutputStream()) {
      os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
    }

    try (InputStream is = conn.getInputStream()) {
      byte[] bytes = new byte[8192];
      StringBuilder sb = new StringBuilder();
      int len;
      while ((len = is.read(bytes)) != -1) {
        sb.append(new String(bytes, 0, len, StandardCharsets.UTF_8));
      }
      return sb.toString();
    } finally {
      conn.disconnect();
    }
  }

  public static String buildNearestNeighborQuery(List<Float> queryVector, int targetHits, String indexNameFilter) throws Exception {
    Map<String, Object> body = new HashMap<>();

    String filterClause = "";
    if (indexNameFilter != null && !indexNameFilter.isEmpty()) {
      filterClause = "indexName contains '" + indexNameFilter + "' and ";
    }

    String yql =
        "select distance(embedding, query_embedding) as vector_distance, * from sources * where " + filterClause +
        "([{\"targetHits\":" + targetHits + "}]nearestNeighbor(embedding, query_embedding))";

    body.put("yql", yql);
    body.put("hits", targetHits);

    Map<String, Object> queryEmbedding = new HashMap<>();
    queryEmbedding.put("values", queryVector);
    body.put("ranking.features.query(query_embedding)", queryEmbedding);
    return JsonUtils.toJsonString(body);
  }
}
