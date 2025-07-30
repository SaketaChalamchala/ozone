package org.apache.hadoop.ozone.s3.endpoint.vectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.hadoop.ozone.s3.endpoint.BucketEndpoint;
import org.apache.hadoop.ozone.s3.endpoint.vectors.request.CreateIndexRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class VectorEndpoint {
    private static final Logger LOG =
        LoggerFactory.getLogger(VectorEndpoint.class);

    @POST
    @Path("/CreateIndex1")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createIndex(CreateIndexRequest request) {
        // Print the request JSON
        LOG.info("Received CreateIndex request:");
        LOG.info("Data Type: " + request.getDataType());
        LOG.info("Dimension: " + request.getDimension());
        LOG.info("Distance Metric: " + request.getDistanceMetric());
        LOG.info("Index Name: " + request.getIndexName());
        LOG.info("Vector Bucket ARN: " + request.getVectorBucketArn());
        LOG.info("Vector Bucket Name: " + request.getVectorBucketName());
        
        if (request.getMetadataConfiguration() != null && 
            request.getMetadataConfiguration().getNonFilterableMetadataKeys() != null) {
            LOG.info("Non-Filterable Metadata Keys: " +
                String.join(", ", request.getMetadataConfiguration().getNonFilterableMetadataKeys()));
        }

        // Return empty response with 200 status code
        return Response.ok().build();
    }


}