package org.apache.hadoop.ozone.s3.endpoint.vectors.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.hadoop.ozone.s3.endpoint.vectors.data.MetadataConfiguration;

public class CreateIndexRequest {
    @JsonProperty(value = "dataType", required = true)
    private String dataType;
    @JsonProperty(value = "dimension", required = true)
    private int dimension;
    @JsonProperty(value = "distanceMetric", required = true)
    private String distanceMetric;
    @JsonProperty(value = "indexName", required = true)
    private String indexName;
    @JsonProperty(value = "metadataConfiguration")
    private MetadataConfiguration metadataConfiguration;
    @JsonProperty(value = "vectorBucketArn")
    private String vectorBucketArn;
    @JsonProperty(value = "vectorBucketName")
    private String vectorBucketName;

    @JsonCreator
    public CreateIndexRequest(@JsonProperty(value = "dataType", required = true) String dataType,
        @JsonProperty(value = "dimension", required = true) int dimension,
        @JsonProperty(value = "distanceMetric", required = true) String distanceMetric,
        @JsonProperty(value = "indexName", required = true) String indexName,
        @JsonProperty(value = "metadataConfiguration") MetadataConfiguration metadataConfiguration,
        @JsonProperty(value = "vectorBucketArn") String vectorBucketArn,
        @JsonProperty(value = "vectorBucketName") String vectorBucketName) {
        this.dataType = dataType;
        this.dimension = dimension;
        this.distanceMetric = distanceMetric;
        this.indexName = indexName;
        this.metadataConfiguration = metadataConfiguration;
        this.vectorBucketArn = vectorBucketArn;
        this.vectorBucketName = vectorBucketName;
    }

    // Getters and setters
    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    public String getDistanceMetric() {
        return distanceMetric;
    }

    public void setDistanceMetric(String distanceMetric) {
        this.distanceMetric = distanceMetric;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public MetadataConfiguration getMetadataConfiguration() {
        return metadataConfiguration;
    }

    public void setMetadataConfiguration(MetadataConfiguration metadataConfiguration) {
        this.metadataConfiguration = metadataConfiguration;
    }

    public String getVectorBucketArn() {
        return vectorBucketArn;
    }

    public void setVectorBucketArn(String vectorBucketArn) {
        this.vectorBucketArn = vectorBucketArn;
    }

    public String getVectorBucketName() {
        return vectorBucketName;
    }

    public void setVectorBucketName(String vectorBucketName) {
        this.vectorBucketName = vectorBucketName;
    }
}
