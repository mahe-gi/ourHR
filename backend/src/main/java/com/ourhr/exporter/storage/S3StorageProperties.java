package com.ourhr.exporter.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for S3 and MinIO compatible object storage.
 */
@Data
@Component
@ConfigurationProperties(prefix = "ourhr.storage.s3")
public class S3StorageProperties {

    /**
     * S3 or MinIO server endpoint URL.
     */
    private String endpoint = "http://localhost:9000";

    /**
     * AWS or S3 region.
     */
    private String region = "us-east-1";

    /**
     * Storage bucket name for documents.
     */
    private String bucket = "ourhr-documents";

    /**
     * S3 access key.
     */
    private String accessKey = "minioadmin";

    /**
     * S3 secret key.
     */
    private String secretKey = "minioadminpassword";

    /**
     * Whether to use path-style bucket access (recommended for MinIO).
     */
    private boolean pathStyleAccess = true;

    /**
     * Whether local filesystem fallback is enabled when S3 is unavailable or in local/test mode.
     */
    private boolean localFallbackEnabled = true;

    /**
     * Base directory on local filesystem for fallback storage.
     */
    private String localStoragePath = "/tmp/ourhr/storage";
}
