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

package org.apache.hadoop.ozone.recon.api;

import static org.apache.hadoop.ozone.recon.ReconConstants.RECON_QUERY_BUCKET;
import static org.apache.hadoop.ozone.recon.ReconConstants.RECON_QUERY_CONTAINER_SIZE;
import static org.apache.hadoop.ozone.recon.ReconConstants.RECON_QUERY_FILE_SIZE;
import static org.apache.hadoop.ozone.recon.ReconConstants.RECON_QUERY_VOLUME;
import static org.apache.ozone.recon.schema.generated.tables.ContainerCountBySizeTable.CONTAINER_COUNT_BY_SIZE;
import static org.apache.ozone.recon.schema.generated.tables.FileCountBySizeTable.FILE_COUNT_BY_SIZE;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.hadoop.ozone.recon.ReconUtils;
import org.apache.ozone.recon.schema.UtilizationSchemaDefinition;
import org.apache.ozone.recon.schema.generated.tables.daos.ContainerCountBySizeDao;
import org.apache.ozone.recon.schema.generated.tables.daos.FileCountBySizeDao;
import org.apache.ozone.recon.schema.generated.tables.pojos.ContainerCountBySize;
import org.apache.ozone.recon.schema.generated.tables.pojos.FileCountBySize;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Record3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Endpoint for querying the counts of a certain file Size.
 */
@Path("/utilization")
@Produces(MediaType.APPLICATION_JSON)
public class UtilizationEndpoint {

  private FileCountBySizeDao fileCountBySizeDao;
  private UtilizationSchemaDefinition utilizationSchemaDefinition;
  private ContainerCountBySizeDao containerCountBySizeDao;
  private static final Logger LOG = LoggerFactory
      .getLogger(UtilizationEndpoint.class);

  @Inject
  public UtilizationEndpoint(FileCountBySizeDao fileCountBySizeDao,
                             ContainerCountBySizeDao containerCountBySizeDao,
                             UtilizationSchemaDefinition
                                 utilizationSchemaDefinition) {
    this.utilizationSchemaDefinition = utilizationSchemaDefinition;
    this.fileCountBySizeDao = fileCountBySizeDao;
    this.containerCountBySizeDao = containerCountBySizeDao;
  }

  /**
   * Return the file counts from Recon DB.
   * @return {@link Response}
   */
  @GET
  @Path("/fileCount")
  public Response getFileCounts(
      @QueryParam(RECON_QUERY_VOLUME)
          String volume,
      @QueryParam(RECON_QUERY_BUCKET)
          String bucket,
      @QueryParam(RECON_QUERY_FILE_SIZE)
          long fileSize
  ) {
    DSLContext dslContext = utilizationSchemaDefinition.getDSLContext();
    List<FileCountBySize> resultSet;
    if (volume != null && bucket != null && fileSize > 0) {
      Record3<String, String, Long> recordToFind = dslContext
          .newRecord(FILE_COUNT_BY_SIZE.VOLUME,
              FILE_COUNT_BY_SIZE.BUCKET,
              FILE_COUNT_BY_SIZE.FILE_SIZE)
          .value1(volume)
          .value2(bucket)
          .value3(fileSize);
      FileCountBySize record = fileCountBySizeDao.findById(recordToFind);
      resultSet = record != null ?
          Collections.singletonList(record) : Collections.emptyList();
    } else if (volume != null && bucket != null) {
      resultSet = dslContext.select().from(FILE_COUNT_BY_SIZE)
          .where(FILE_COUNT_BY_SIZE.VOLUME.eq(volume))
          .and(FILE_COUNT_BY_SIZE.BUCKET.eq(bucket))
          .fetchInto(FileCountBySize.class);
    } else if (volume != null) {
      resultSet = fileCountBySizeDao.fetchByVolume(volume);
    } else {
      // fetch all records
      resultSet = fileCountBySizeDao.findAll();
    }
    return Response.ok(resultSet).build();
  }

  /**
   * Return the container size counts from Recon DB.
   *
   * @return {@link Response}
   */
  @GET
  @Path("/containerCount")
  public Response getContainerCounts(
      @QueryParam(RECON_QUERY_CONTAINER_SIZE)
          long upperBound) {
    DSLContext dslContext = utilizationSchemaDefinition.getDSLContext();
    Long containerSizeUpperBound =
        ReconUtils.getContainerSizeUpperBound(upperBound);
    List<ContainerCountBySize> resultSet;
    try {
      if (upperBound > 0) {
        // Get the current count from database and update
        Record1<Long> recordToFind =
            dslContext.newRecord(
                    CONTAINER_COUNT_BY_SIZE.CONTAINER_SIZE)
                .value1(containerSizeUpperBound);
        ContainerCountBySize record =
            containerCountBySizeDao.findById(recordToFind.value1());
        resultSet = record != null ?
            Collections.singletonList(record) : Collections.emptyList();
      } else {
        // fetch all records having values greater than zero
        resultSet = containerCountBySizeDao.findAll().stream()
            .filter(record -> record.getCount() > 0)
            .collect(Collectors.toList());
      }
      return Response.ok(resultSet).build();
    } catch (Exception e) {
      // Log the exception and return a server error response
      LOG.error("Error retrieving container counts", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

}
