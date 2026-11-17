package com.ourhr.exporter.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object representing the result of a document upload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {

    /**
     * Unique sanitized storage key (e.g. {companyId}/{directory}/{docCategory}_{uuid}.{ext}).
     */
    private String fileKey;

    /**
     * Direct or pre-signed URL for accessing the uploaded document.
     */
    private String documentUrl;

    /**
     * Resolved MIME content type of the file.
     */
    private String contentType;

    /**
     * File size in bytes.
     */
    private long sizeBytes;
}
