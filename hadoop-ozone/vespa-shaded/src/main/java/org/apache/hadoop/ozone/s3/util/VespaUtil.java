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

import com.fasterxml.jackson.databind.JsonNode;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.hadoop.hdds.server.JsonUtils;
import org.apache.hadoop.hdfs.web.URLConnectionFactory;
import org.apache.hadoop.ozone.om.exceptions.OMException;
import org.apache.hadoop.ozone.om.helpers.OzoneVectorIndex;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class VespaUtil {

  public static String getSchemaName(int vectorSize, String distanceMetric) {
    return "vector_schema_" + vectorSize + "_" + distanceMetric;
  }

  public static void writeHostsXml(Map<String, String> hostAliasMap, File outputFile)
      throws ParserConfigurationException, TransformerException {
    // Create the XML document
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc = dBuilder.newDocument();

    // Root element: <hosts>
    Element rootElement = doc.createElement("hosts");
    doc.appendChild(rootElement);

    // For each host entry
    for (Map.Entry<String, String> entry : hostAliasMap.entrySet()) {
      // <host name="hostname">
      Element hostElement = doc.createElement("host");
      hostElement.setAttribute("name", entry.getKey());
      rootElement.appendChild(hostElement);

      // <alias>alias</alias>
      Element aliasElement = doc.createElement("alias");
      aliasElement.setTextContent(entry.getValue());
      hostElement.appendChild(aliasElement);
    }

    // Write XML to file with pretty print
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

    DOMSource source = new DOMSource(doc);
    StreamResult result = new StreamResult(outputFile);
    transformer.transform(source, result);
  }

  public static void writeServicesXml(List<String> hostAliases, String adminAlias, List<String> docs,
      File outputFile) throws ParserConfigurationException, TransformerException {
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc = dBuilder.newDocument();

    // Root element: <services version="1.0">
    Element servicesEl = doc.createElement("services");
    servicesEl.setAttribute("version", "1.0");
    doc.appendChild(servicesEl);

    // <admin version="2.0">
    Element adminEl = doc.createElement("admin");
    adminEl.setAttribute("version", "2.0");
    servicesEl.appendChild(adminEl);

    // <adminserver hostalias="..."/>
    Element adminServerEl = doc.createElement("adminserver");
    adminServerEl.setAttribute("hostalias", adminAlias);
    adminEl.appendChild(adminServerEl);

    // <container id="default" version="1.0">
    Element containerEl = doc.createElement("container");
    containerEl.setAttribute("id", "default");
    containerEl.setAttribute("version", "1.0");
    servicesEl.appendChild(containerEl);

    // <nodes> inside container
    Element containerNodesEl = doc.createElement("nodes");
    containerEl.appendChild(containerNodesEl);

    // First node in container is the adminAlias
    Element containerNodeEl = doc.createElement("node");
    containerNodeEl.setAttribute("hostalias", adminAlias);
    containerNodesEl.appendChild(containerNodeEl);

    // <search/> and <document-api/>
    containerEl.appendChild(doc.createElement("search"));
    containerEl.appendChild(doc.createElement("document-api"));

    // <content id="ozone-sko-content" version="1.0">
    Element contentEl = doc.createElement("content");
    contentEl.setAttribute("id", "ozone-sko-vector-content");
    contentEl.setAttribute("version", "1.0");
    servicesEl.appendChild(contentEl);

    // <redundancy>3</redundancy>
    Element redundancyEl = doc.createElement("redundancy");
    redundancyEl.setTextContent("3");
    contentEl.appendChild(redundancyEl);

    // <documents> section
    Element documentsEl = doc.createElement("documents");
    for (String docName : docs) {
      Element documentEl = doc.createElement("document");
      documentEl.setAttribute("type", docName);
      documentEl.setAttribute("mode", "index");
      documentsEl.appendChild(documentEl);
    }
    contentEl.appendChild(documentsEl);

    // <nodes> for content
    Element contentNodesEl = doc.createElement("nodes");
    for (int i = 0; i < hostAliases.size(); i++) {
      Element nodeEl = doc.createElement("node");
      nodeEl.setAttribute("hostalias", hostAliases.get(i));
      nodeEl.setAttribute("distribution-key", String.valueOf(i));
      contentNodesEl.appendChild(nodeEl);
    }
    contentEl.appendChild(contentNodesEl);

    // Write to file
    TransformerFactory tf = TransformerFactory.newInstance();
    Transformer transformer = tf.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

    transformer.transform(new DOMSource(doc), new StreamResult(outputFile));
  }

  public static void writeSchemaFile(String schemaName, int vectorSize, String distanceMetric, File outputFile)
      throws IOException {
    // schema (.sd) with vector, indexName, and non-searchable JSON string
    String schemaContent =
        "schema " + schemaName + " {\n" +
            "  document " + schemaName + " {\n" +
            "    field indexName type string {\n" +
            "      indexing: summary | attribute\n" +
            "    }\n" +
            "    field embedding type tensor<float>(x[" + vectorSize + "]) {\n" +
            "      indexing: summary | attribute | index\n" +
            "      attribute {\n" +
            "        distance-metric: " + distanceMetric + "\n" +
            "      }\n" +
            "      index {\n" +
            "         hnsw {\n" +
            "           max-links-per-node: 32\n" +
            "           neighbors-to-explore-at-insert: 200\n" +
            "         }\n" +
            "      }\n" +
            "    }\n" +
            "    field metadata type string {\n" +
            "      indexing: input metadata | summary\n" +
            "    }\n" +
            "  }\n" +
            "  fieldset default {\n" +
            "    fields: indexName\n" +
            "  }\n" +
            "  rank-profile rank_embedding_" + schemaName + " {\n" +
            "    num-threads-per-search: 1\n" +
            "    match-features: distance(field, embedding)\n" +
            "    inputs {\n" +
            "    query(q_embedding_" + schemaName + ")  tensor<float>(x[" + vectorSize + "])\n" +
            "    }\n" +
            "    first-phase {\n" +
            "      expression: closeness(field, embedding)\n" +
            "    }\n" +
            "  }" +
            "}\n";
    Files.write(outputFile.toPath(), schemaContent.getBytes());
  }


  public static synchronized void deploySchema(URLConnectionFactory urlConnectionFactory, String host, int port,
      List<OzoneVectorIndex> vectorIndices, Map<String, String> hostMap) throws IOException {
    try {
      String vespaHost = host + ":" + port;
      Path tempDir = Files.createTempDirectory("vespa_app");
      Path appDir = Files.createDirectories(tempDir.resolve("ozone-vector"));
      Path schemasDir = Files.createDirectories(appDir.resolve("schemas"));
      writeHostsXml(hostMap, appDir.resolve("hosts.xml").toFile());
      writeServicesXml(new ArrayList<>(hostMap.values()), hostMap.get(new URL(host).getHost()),
          vectorIndices.stream().map(OzoneVectorIndex::getIndexName).collect(Collectors.toList()),
          appDir.resolve("services.xml").toFile());

      for (OzoneVectorIndex vectorIndex : vectorIndices) {
        writeSchemaFile(vectorIndex.getIndexName(), vectorIndex.getDimension(), vectorIndex.getDistanceMetric(),
            schemasDir.resolve(vectorIndex.getIndexName() + ".sd").toFile());
      }

      Path appZip = Files.createTempFile("ozone-vector", ".zip");
      zipDirectory(appDir, appZip);
      String tenant = "default";
      String sessionId = createSession(urlConnectionFactory, appZip.toFile(), vespaHost, tenant);
      prepareSession(urlConnectionFactory, vespaHost, sessionId, tenant);
      activateSession(urlConnectionFactory, vespaHost, sessionId, tenant);
    } catch (ParserConfigurationException | TransformerException | IOException e) {
      throw new OMException(e, OMException.ResultCodes.INTERNAL_ERROR);
    }
  }

  private static String createSession(URLConnectionFactory connectionFactory, File zipFile, String vespaHost,
      String tenant) throws IOException {
    URL url = new URL(vespaHost + "/application/v2/tenant/" + tenant + "/session");
    HttpURLConnection conn = (HttpURLConnection) connectionFactory.openConnection(url);
    conn.setDoOutput(true);
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/zip");

    try (OutputStream os = conn.getOutputStream()) {
      Files.copy(zipFile.toPath(), os);
    }
    String response = readResponse(conn);
    System.out.println("Create Session Response: " + response);
    return JsonUtils.fromJson(response, JsonNode.class).get("session-id").asText();
  }

  private static void prepareSession(URLConnectionFactory connectionFactory, String vespaHost, String sessionId,
      String tenant) throws IOException {
    URL url = new URL(vespaHost + "/application/v2/tenant/"+ tenant + "/session/" + sessionId + "/prepared" +
        "?applicationName=default");
    HttpURLConnection conn = (HttpURLConnection) connectionFactory.openConnection(url);
    conn.setRequestMethod("PUT");
    conn.setRequestProperty("User-Agent", "vespa-deploy");
    conn.setDoOutput(true);
    try (OutputStream os = conn.getOutputStream()) {
      os.write(new byte[0]);
    }
    handleResponse(conn, "prepare session");
  }

  private static String handleResponse(HttpURLConnection conn, String operation) throws IOException {
    int responseCode = conn.getResponseCode();
    if (responseCode / 100 != 2) {
      StringBuilder sb = new StringBuilder();
      try (InputStream is = (responseCode >= 200 && responseCode < 400)
          ? conn.getInputStream()
          : conn.getErrorStream();
           BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
        String line;
        while ((line = reader.readLine()) != null) {
          sb.append(line).append("\n");
        }
      }
      throw new IOException("Failed to " + operation + ": " + sb + "\t code: " + responseCode);
    }
    return conn.getResponseMessage();
  }

  private static void activateSession(URLConnectionFactory urlConnectionFactory, String vespaHost, String sessionId,
   String tenant) throws IOException {
    URL url = new URL(vespaHost + "/application/v2/tenant/" + tenant + "/session/" + sessionId + "/active");
    HttpURLConnection conn = (HttpURLConnection) urlConnectionFactory.openConnection(url);
    conn.setRequestMethod("PUT");
    conn.setRequestProperty("User-Agent", "vespa-deploy");
    conn.setDoOutput(true);
    try (OutputStream os = conn.getOutputStream()) {
      os.write(new byte[0]);
    }
    handleResponse(conn, "activate session");
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
    handleResponse(conn, "post json");
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

  public static String buildNearestNeighborQuery(List<Float> queryVector, int targetHits, String schemaName,
      String indexNameFilter) throws Exception {
    Map<String, Object> body = new HashMap<>();

    String filterClause = "";
    if (indexNameFilter != null && !indexNameFilter.isEmpty()) {
      filterClause = "indexName contains '" + indexNameFilter + "' and ";
    }

    String yql =
        "select * from " + schemaName + " where "
            + filterClause + "({targetHits:" + targetHits + "}nearestNeighbor(embedding, q_embedding_"+ schemaName + "))";

    body.put("yql", yql);
    body.put("hits", targetHits);
    body.put("ranking", "rank_embedding_" + schemaName);
    Map<String, Object> queryEmbedding = new HashMap<>();
    queryEmbedding.put("query(q_embedding_" + schemaName + ")", queryVector);
    body.put("input", queryEmbedding);
    return JsonUtils.toJsonString(body);
  }
}
