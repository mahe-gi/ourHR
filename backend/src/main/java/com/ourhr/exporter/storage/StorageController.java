package com.ourhr.exporter.storage;

import com.ourhr.common.ApiResponse;
import com.ourhr.exporter.storage.dto.FileUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;

/**
 * REST controller for document storage and retrieval.
 * Provides authenticated document upload for employees, HR, and founders,
 * and read-only access for compliance review by Chartered Accountants and Finance.
 */
@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
@Tag(name = "Storage & KYC Documents", description = "Document storage and KYC file retrieval endpoints")
public class StorageController {

    private final FileStorageService fileStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'EMPLOYEE')")
    @Operation(summary = "Upload KYC or statutory document",
            description = "Uploads document file (.pdf, .jpg, .jpeg, .png up to 5MB) with automatic sensitive ID redaction.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @Parameter(description = "Document file to upload")
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Document category (e.g. KYC_PAN, KYC_AADHAAR, KYC_CHEQUE, APPOINTMENT_LETTER, FORM_12B)")
            @RequestParam(value = "docCategory", defaultValue = "GENERAL") String docCategory
    ) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File content cannot be empty");
        }
        if (file.getSize() > LocalFallbackFileStorageService.MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds maximum allowed limit of 5MB");
        }

        String directory = (docCategory != null && !docCategory.isBlank()) ? docCategory.toLowerCase(Locale.ROOT) : "general";
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = directory + ".pdf";
        }

        byte[] content = file.getBytes();
        String contentType = file.getContentType();

        String fileKey = fileStorageService.uploadFile(directory, originalFilename, content, contentType);
        String documentUrl = fileStorageService.generatePresignedUrl(fileKey, 3600);
        String resolvedContentType = determineContentType(fileKey, contentType);

        FileUploadResponse response = FileUploadResponse.builder()
                .fileKey(fileKey)
                .documentUrl(documentUrl)
                .contentType(resolvedContentType)
                .sizeBytes(file.getSize())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/download")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'FINANCE', 'CA', 'EMPLOYEE')")
    @Operation(summary = "Download stored document",
            description = "Retrieves raw document bytes with appropriate Content-Disposition and MIME Content-Type headers.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<byte[]> downloadFile(
            @Parameter(description = "Unique storage file key")
            @RequestParam("fileKey") String fileKey,
            @Parameter(description = "Whether to display document inline in browser (defaults to false for download attachment)")
            @RequestParam(value = "inline", defaultValue = "false") boolean inline
    ) {
        if (fileKey == null || fileKey.isBlank()) {
            throw new IllegalArgumentException("File key cannot be null or empty");
        }

        byte[] content = fileStorageService.downloadFile(fileKey);
        String contentType = determineContentType(fileKey, null);
        String filename = extractFilename(fileKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentLength(content.length);

        if (inline) {
            headers.setContentDisposition(ContentDisposition.inline().filename(filename).build());
        } else {
            headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        }

        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR')")
    @Operation(summary = "Delete stored document",
            description = "Removes a document from storage by file key.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @Parameter(description = "Unique storage file key to delete")
            @RequestParam("fileKey") String fileKey
    ) {
        if (fileKey == null || fileKey.isBlank()) {
            throw new IllegalArgumentException("File key cannot be null or empty");
        }
        fileStorageService.deleteFile(fileKey);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private String determineContentType(String fileKey, String fallbackContentType) {
        if (fileKey != null) {
            String lower = fileKey.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".pdf")) {
                return MediaType.APPLICATION_PDF_VALUE;
            } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                return MediaType.IMAGE_JPEG_VALUE;
            } else if (lower.endsWith(".png")) {
                return MediaType.IMAGE_PNG_VALUE;
            }
        }
        if (fallbackContentType != null && !fallbackContentType.isBlank()) {
            return fallbackContentType;
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private String extractFilename(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            return "document.pdf";
        }
        int lastSlash = Math.max(fileKey.lastIndexOf('/'), fileKey.lastIndexOf('\\'));
        if (lastSlash >= 0 && lastSlash < fileKey.length() - 1) {
            return fileKey.substring(lastSlash + 1);
        }
        return fileKey;
    }
}
