package com.ourhr.exporter.storage;

/**
 * Storage service interface for storing, retrieving, and managing HR and KYC documents.
 * Implementations enforce the Sensitive ID Redaction Law and restrict allowed file types.
 */
public interface FileStorageService {

    /**
     * Uploads document content to the designated directory and returns a secure sanitized storage key.
     *
     * @param directory        target folder or category directory
     * @param originalFilename original client file name (sanitized internally to remove sensitive IDs)
     * @param content          raw file bytes
     * @param contentType      MIME content type
     * @return unique sanitized storage file key
     */
    String uploadFile(String directory, String originalFilename, byte[] content, String contentType);

    /**
     * Downloads document content bytes corresponding to the given file key.
     *
     * @param fileKey unique storage file key
     * @return raw file bytes
     */
    byte[] downloadFile(String fileKey);

    /**
     * Deletes a document from storage by file key.
     *
     * @param fileKey unique storage file key
     */
    void deleteFile(String fileKey);

    /**
     * Generates a pre-signed or accessible retrieval URL for the document.
     *
     * @param fileKey           unique storage file key
     * @param expirationSeconds validity duration in seconds
     * @return pre-signed or accessible URL string
     */
    String generatePresignedUrl(String fileKey, long expirationSeconds);
}
