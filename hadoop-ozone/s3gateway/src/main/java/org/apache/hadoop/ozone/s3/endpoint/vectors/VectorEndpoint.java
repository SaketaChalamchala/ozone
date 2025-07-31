package org.apache.hadoop.ozone.s3.endpoint.vectors;

import static org.apache.hadoop.ozone.s3.exception.S3ErrorTable.newError;
import static org.apache.hadoop.ozone.s3.util.S3Consts.S3_VECTORS_PATH;

import io.milvus.client.MilvusServiceClient;
import java.io.IOException;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.hadoop.ozone.audit.S3GAction;
import org.apache.hadoop.ozone.om.exceptions.OMException;
import org.apache.hadoop.ozone.om.helpers.BucketLayout;
import org.apache.hadoop.ozone.s3.endpoint.EndpointBase;
import org.apache.hadoop.ozone.s3.endpoint.vectors.request.CreateIndexRequest;
import org.apache.hadoop.ozone.s3.endpoint.vectors.request.CreateVectorBucketRequest;
import org.apache.hadoop.ozone.s3.exception.OS3Exception;
import org.apache.hadoop.ozone.s3.exception.S3ErrorTable;
import org.apache.hadoop.ozone.s3.util.S3Consts;
import org.apache.hadoop.util.Time;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/" + S3_VECTORS_PATH)
public class VectorEndpoint extends EndpointBase {
    private static final Logger LOG =
        LoggerFactory.getLogger(VectorEndpoint.class);

    @Inject
    MilvusServiceClient milvusServiceClient;

    public static boolean isMatch(String path) {
        return S3Consts.VALID_PATH_PATTERN.matcher(path).matches();
    }

    @POST
    @Path("/CreateVectorBucket")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createVectorBucket(CreateVectorBucketRequest request) throws IOException, OS3Exception {
        long startNanos = Time.monotonicNowNanos();
        S3GAction s3GAction = S3GAction.CREATE_BUCKET;
        try {
            createS3Bucket(request.getVectorBucketName(), BucketLayout.VECTOR_BUCKET);
            AUDIT.logWriteSuccess(
                auditMessageForSuccess(s3GAction).build());
            getMetrics().updateCreateBucketSuccessStats(startNanos);
            return Response.status(HttpStatus.SC_OK).build();
        } catch (OMException exception) {
            auditWriteFailure(s3GAction, exception);
            getMetrics().updateCreateBucketFailureStats(startNanos);
            if (exception.getResult() == OMException.ResultCodes.INVALID_BUCKET_NAME) {
                throw newError(S3ErrorTable.INVALID_BUCKET_NAME, request.getVectorBucketName(), exception);
            }
            throw exception;
        } catch (Exception ex) {
            AUDIT.logWriteFailure(
                auditMessageForFailure(s3GAction, ex).build());
            throw ex;
        }
    }

    @POST
    @Path("/CreateIndex")
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


    @Override
    public void init() {

    }
}
