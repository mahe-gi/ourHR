package com.ourhr.exporter.storage;

import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {

    @TempDir
    Path tempStorageDir;

    private LocalFallbackFileStorageService storageService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        S3StorageProperties properties = new S3StorageProperties();
        properties.setLocalStoragePath(tempStorageDir.toString());
        properties.setLocalFallbackEnabled(true);

        storageService = new LocalFallbackFileStorageService(properties);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Upload document stores content and returns formatted file key")
    void shouldUploadAndDownloadFileSuccessfully() {
        byte[] content = "%PDF-1.4 test document content".getBytes();
        String fileKey = storageService.uploadFile("kyc", "appointment_letter.pdf", content, "application/pdf");

        assertThat(fileKey).isNotNull();
        assertThat(fileKey).startsWith(tenantId.toString() + "/kyc/");
        assertThat(fileKey).endsWith(".pdf");

        byte[] downloaded = storageService.downloadFile(fileKey);
        assertThat(downloaded).isEqualTo(content);
    }

    @Test
    @DisplayName("Sensitive ID Redaction Law: Raw Aadhaar and PAN in original filenames are redacted in storage key")
    void shouldRedactAadhaarAndPanInFileKey() {
        byte[] content = "%PDF-1.4 aadhaar content".getBytes();

        // Raw 12-digit Aadhaar in filename
        String fileKeyAadhaar = storageService.uploadFile("kyc", "aadhaar-999988887777.pdf", content, "application/pdf");
        assertThat(fileKeyAadhaar).doesNotContain("999988887777");
        assertThat(fileKeyAadhaar).contains("redacted");

        // Raw 12-digit Aadhaar as entire filename
        String fileKeyRaw = storageService.uploadFile("kyc", "999988887777.pdf", content, "application/pdf");
        assertThat(fileKeyRaw).doesNotContain("999988887777");
        assertThat(fileKeyRaw).contains("redacted");

        // Hyphenated Aadhaar in directory
        String fileKeyFormatted = storageService.uploadFile("aadhaar-1234-5678-9012", "doc.pdf", content, "application/pdf");
        assertThat(fileKeyFormatted).doesNotContain("1234-5678-9012");
        assertThat(fileKeyFormatted).contains("redacted");

        // PAN number in filename
        String fileKeyPan = storageService.uploadFile("kyc", "pan-ABCDE1234F.pdf", content, "application/pdf");
        assertThat(fileKeyPan).doesNotContain("ABCDE1234F");
        assertThat(fileKeyPan).contains("redacted");
    }

    @Test
    @DisplayName("Path traversal attacks with .. or leading slashes are blocked")
    void shouldBlockPathTraversalAttacks() {
        assertThatThrownBy(() -> storageService.downloadFile("../../../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("directory traversal is prohibited");

        assertThatThrownBy(() -> storageService.downloadFile("/etc/shadow"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("directory traversal is prohibited");

        assertThatThrownBy(() -> storageService.downloadFile("\\windows\\system32"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("directory traversal is prohibited");

        assertThatThrownBy(() -> storageService.deleteFile("../secret.txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("directory traversal is prohibited");
    }

    @Test
    @DisplayName("File size exceeding 5MB is rejected with IllegalArgumentException")
    void shouldRejectFilesExceedingFiveMegabytes() {
        byte[] largeContent = new byte[(int) (LocalFallbackFileStorageService.MAX_FILE_SIZE_BYTES + 1024)];

        assertThatThrownBy(() -> storageService.uploadFile("kyc", "large.pdf", largeContent, "application/pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File size exceeds maximum allowed limit of 5MB");
    }

    @Test
    @DisplayName("Empty content is rejected with IllegalArgumentException")
    void shouldRejectEmptyFileContent() {
        assertThatThrownBy(() -> storageService.uploadFile("kyc", "empty.pdf", new byte[0], "application/pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File content cannot be empty");

        assertThatThrownBy(() -> storageService.uploadFile("kyc", "null.pdf", null, "application/pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File content cannot be empty");
    }

    @Test
    @DisplayName("Unsupported file extensions are rejected")
    void shouldRejectUnsupportedFileExtensions() {
        byte[] content = "malicious script".getBytes();

        assertThatThrownBy(() -> storageService.uploadFile("kyc", "script.sh", content, "application/x-sh"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file type");

        assertThatThrownBy(() -> storageService.uploadFile("kyc", "program.exe", content, "application/octet-stream"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file type");
    }

    @Test
    @DisplayName("Supported image formats (.jpg, .jpeg, .png) are accepted")
    void shouldAcceptSupportedImageFormats() {
        byte[] imageBytes = new byte[]{1, 2, 3, 4};

        String jpgKey = storageService.uploadFile("kyc", "photo.jpg", imageBytes, "image/jpeg");
        assertThat(jpgKey).endsWith(".jpg");

        String jpegKey = storageService.uploadFile("kyc", "photo.jpeg", imageBytes, "image/jpeg");
        assertThat(jpegKey).endsWith(".jpeg");

        String pngKey = storageService.uploadFile("kyc", "signature.png", imageBytes, "image/png");
        assertThat(pngKey).endsWith(".png");
    }

    @Test
    @DisplayName("Download throws ResourceNotFoundException when file does not exist")
    void shouldThrowResourceNotFoundExceptionWhenFileNotFound() {
        String missingKey = tenantId + "/kyc/missing_file_key.pdf";

        assertThatThrownBy(() -> storageService.downloadFile(missingKey))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("File not found for key");
    }

    @Test
    @DisplayName("Delete removes the file from local storage")
    void shouldDeleteFileSuccessfully() {
        byte[] content = "test delete".getBytes();
        String fileKey = storageService.uploadFile("kyc", "to_delete.pdf", content, "application/pdf");

        assertThat(storageService.downloadFile(fileKey)).isNotEmpty();

        storageService.deleteFile(fileKey);

        assertThatThrownBy(() -> storageService.downloadFile(fileKey))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Presigned URL generation encodes key in download URL")
    void shouldGeneratePresignedUrl() {
        String fileKey = tenantId + "/kyc/doc_sample.pdf";
        String presignedUrl = storageService.generatePresignedUrl(fileKey, 3600);

        assertThat(presignedUrl).startsWith("/api/v1/storage/download?fileKey=");
        assertThat(presignedUrl).contains(tenantId.toString());
    }

    @Test
    @DisplayName("Blank file keys throw IllegalArgumentException")
    void shouldThrowOnBlankFileKeys() {
        assertThatThrownBy(() -> storageService.downloadFile(""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> storageService.downloadFile(null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> storageService.deleteFile("   "))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> storageService.generatePresignedUrl(null, 3600))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
