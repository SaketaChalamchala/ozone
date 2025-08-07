package org.apache.hadoop.ozone.s3.endpoint.vectors;

import static org.apache.hadoop.ozone.s3.exception.S3ErrorTable.newError;
import static org.apache.hadoop.ozone.s3.util.S3Consts.S3_VECTORS_PATH;

import io.milvus.client.MilvusServiceClient;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.hadoop.ozone.audit.S3GAction;
import org.apache.hadoop.ozone.client.OzoneBucket;
import org.apache.hadoop.ozone.om.exceptions.OMException;
import org.apache.hadoop.ozone.om.helpers.BucketLayout;
import org.apache.hadoop.ozone.s3.endpoint.EndpointBase;
import org.apache.hadoop.ozone.s3.endpoint.ObjectEndpoint;
import org.apache.hadoop.ozone.s3.endpoint.vectors.data.ListOutputVector;
import org.apache.hadoop.ozone.s3.endpoint.vectors.data.PutInputVector;
import org.apache.hadoop.ozone.s3.endpoint.vectors.data.QueryOutputVector;
import org.apache.hadoop.ozone.s3.endpoint.vectors.data.VectorData;
import org.apache.hadoop.ozone.s3.endpoint.vectors.request.CreateIndexRequest;
import org.apache.hadoop.ozone.s3.endpoint.vectors.request.CreateVectorBucketRequest;
import org.apache.hadoop.ozone.s3.endpoint.vectors.request.PutVectorRequest;
import org.apache.hadoop.ozone.s3.endpoint.vectors.request.QueryVectorRequest;
import org.apache.hadoop.ozone.s3.endpoint.vectors.store.VectorStore;
import org.apache.hadoop.ozone.s3.exception.OS3Exception;
import org.apache.hadoop.ozone.s3.exception.S3ErrorTable;
import org.apache.hadoop.ozone.s3.util.S3Consts;
import org.apache.hadoop.ozone.s3.util.VespaUtil;
import org.apache.hadoop.util.Time;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/" + S3_VECTORS_PATH)
public class VectorEndpoint extends EndpointBase {
    private static final Logger LOG =
        LoggerFactory.getLogger(VectorEndpoint.class);

    @Inject
    VectorStore vectorStore;

    public static boolean isMatch(String path) {
        return S3Consts.VALID_PATH_PATTERN.matcher(path).matches();
    }

    private static String getVectorBucketSchemaMetadata(String vectorBucketName, String indexName) {
        return "ozoneVectorBucket/" + vectorBucketName + "/" + indexName;
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
    public Response createIndex(CreateIndexRequest request) throws Exception {
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
        String indexSchemaName = vectorStore.createIndex(request.getVectorBucketName(), request.getIndexName(),
            request.getDistanceMetric(), request.getDimension());
        addS3BucketMetadata(request.getVectorBucketName(),
            "ozoneVectorBucket/" + request.getVectorBucketName() + "/" + request.getIndexName(),
            indexSchemaName);
        // Return empty response with 200 status code
        return Response.ok().build();
    }

    @POST
    @Path("/PutVectors")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putVectors(PutVectorRequest request) throws Exception {
        String metadataKey = getVectorBucketSchemaMetadata(request.getVectorBucketName(), request.getIndexName());
        OzoneBucket bucket = getBucket(request.getVectorBucketName());
        String indexSchemaName = bucket.getMetadata().get(metadataKey);
        for (PutInputVector putInputVector : request.getVectors()) {
            vectorStore.putVectorData(request.getVectorBucketName(), request.getIndexName(), indexSchemaName,
                putInputVector.getKey(), putInputVector.getMetadata(), putInputVector.getData());
        }
        return Response.ok().build();
    }

    @POST
    @Path("/QueryVectors")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response queryVectors(QueryVectorRequest request) throws Exception {
        List<ListOutputVector> outputVectors = vectorStore.getVectorData(request.getVectorBucketName(), request.getIndexName(),
            request.getQueryVector(),
            request.getTopK());
        return Response.ok(Collections.singletonMap("vectors", outputVectors), MediaType.APPLICATION_JSON_TYPE).build();
    }


    @Override
    public void init() {

    }
}
