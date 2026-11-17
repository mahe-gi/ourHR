package com.ourhr.exporter.storage;

import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.common.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Local filesystem file storage service implementation providing resilient document storage.
 * Enforces the Sensitive ID Redaction Law, 5MB file size limit, and allowed extension validation.
 */
@Component
public class LocalFallbackFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFallbackFileStorageService.class);

    public static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L; // 5 MB
    public static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".jpg", ".jpeg", ".png", ".txt", ".csv");

    private static final Pattern AADHAAR_PATTERN = Pattern.compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b|\\b\\d{12}\\b");
    private static final Pattern PAN_PATTERN = Pattern.compile("\\b[A-Za-z]{5}[0-9]{4}[A-Za-z]\\b");

    private final S3StorageProperties properties;
    private final Path rootPath;

    public LocalFallbackFileStorageService(S3StorageProperties properties) {
        this.properties = properties;
        this.rootPath = Path.of(properties.getLocalStoragePath()).toAbsolutePath().normalize();
        initializeDirectory();
    }

    private void initializeDirectory() {
        try {
            Files.createDirectories(this.rootPath);
            log.info("Initialized local document storage directory at: {}", this.rootPath);
        } catch (IOException ex) {
            log.error("Failed to initialize local document storage directory at: {}", this.rootPath, ex);
            throw new IllegalStateException("Could not create local storage directory", ex);
        }
    }

    @Override
    public String uploadFile(String directory, String originalFilename, byte[] content, String contentType) {
        validateContent(content);
        String extension = validateAndExtractExtension(originalFilename, contentType);
        String fileKey = generateSecureFileKey(directory, originalFilename, extension);

        Path targetPath = resolveAndVerifyPath(fileKey);
        try {
            if (targetPath.getParent() != null) {
                Files.createDirectories(targetPath.getParent());
            }
            Files.write(targetPath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            log.info("Successfully stored document with key: {}", maskKeyForLog(fileKey));
            return fileKey;
        } catch (IOException ex) {
            log.error("Failed to write document to local storage for key: {}", maskKeyForLog(fileKey), ex);
            throw new IllegalStateException("Failed to write file to local storage", ex);
        }
    }

    @Override
    public byte[] downloadFile(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            throw new IllegalArgumentException("File key cannot be null or empty");
        }
        Path targetPath = resolveAndVerifyPath(fileKey);
        if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            log.warn("Document not found for key: {}", maskKeyForLog(fileKey));
            throw new ResourceNotFoundException("File not found for key: " + maskKeyForLog(fileKey));
        }
        try {
            return Files.readAllBytes(targetPath);
        } catch (IOException ex) {
            log.error("Failed to read document from local storage for key: {}", maskKeyForLog(fileKey), ex);
            throw new IllegalStateException("Failed to read file from local storage", ex);
        }
    }

    @Override
    public void deleteFile(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            throw new IllegalArgumentException("File key cannot be null or empty");
        }
        Path targetPath = resolveAndVerifyPath(fileKey);
        try {
            boolean deleted = Files.deleteIfExists(targetPath);
            if (deleted) {
                log.info("Deleted document with key: {}", maskKeyForLog(fileKey));
            } else {
                log.warn("Document to delete was not found for key: {}", maskKeyForLog(fileKey));
            }
        } catch (IOException ex) {
            log.error("Failed to delete document for key: {}", maskKeyForLog(fileKey), ex);
            throw new IllegalStateException("Failed to delete file from local storage", ex);
        }
    }

    @Override
    public String generatePresignedUrl(String fileKey, long expirationSeconds) {
        if (fileKey == null || fileKey.isBlank()) {
            throw new IllegalArgumentException("File key cannot be null or empty");
        }
        String encodedKey = URLEncoder.encode(fileKey, StandardCharsets.UTF_8);
        return "/api/v1/storage/download?fileKey=" + encodedKey;
    }

    private void validateContent(byte[] content) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("File content cannot be empty");
        }
        if (content.length > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds maximum allowed limit of 5MB");
        }
    }

    private String validateAndExtractExtension(String originalFilename, String contentType) {
        String ext = null;
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        }

        if (ext == null || !ALLOWED_EXTENSIONS.contains(ext)) {
            if (contentType != null) {
                String normalizedType = contentType.toLowerCase(Locale.ROOT).trim();
                if (normalizedType.contains("pdf")) {
                    ext = ".pdf";
                } else if (normalizedType.contains("jpeg") || normalizedType.contains("jpg")) {
                    ext = ".jpg";
                } else if (normalizedType.contains("png")) {
                    ext = ".png";
                } else if (normalizedType.contains("text/plain") || normalizedType.contains("txt")) {
                    ext = ".txt";
                } else if (normalizedType.contains("csv")) {
                    ext = ".csv";
                }
            }
        }

        if (ext == null || !ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Unsupported file type. Allowed extensions: .pdf, .jpg, .jpeg, .png, .txt, .csv");
        }
        return ext;
    }

    private String generateSecureFileKey(String directory, String originalFilename, String extension) {
        UUID tenantId = TenantContext.getTenantId();
        String tenantPrefix = (tenantId != null) ? tenantId.toString() : "default";

        String cleanDirectory = sanitizePathSegment(directory, "documents");
        String cleanPrefix = sanitizeFilenamePrefix(originalFilename);

        String randomIdentifier = UUID.randomUUID().toString();
        return tenantPrefix + "/" + cleanDirectory + "/" + cleanPrefix + "_" + randomIdentifier + extension;
    }

    private String sanitizePathSegment(String segment, String fallback) {
        if (segment == null || segment.isBlank()) {
            return fallback;
        }
        String sanitized = segment.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase(Locale.ROOT);
        sanitized = AADHAAR_PATTERN.matcher(sanitized).replaceAll("redacted");
        sanitized = PAN_PATTERN.matcher(sanitized).replaceAll("redacted");
        return sanitized.isBlank() ? fallback : sanitized;
    }

    private String sanitizeFilenamePrefix(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "doc";
        }
        String baseName = originalFilename;
        int lastDot = baseName.lastIndexOf('.');
        if (lastDot > 0) {
            baseName = baseName.substring(0, lastDot);
        }
        baseName = AADHAAR_PATTERN.matcher(baseName).replaceAll("redacted");
        baseName = PAN_PATTERN.matcher(baseName).replaceAll("redacted");
        String sanitized = baseName.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase(Locale.ROOT);
        if (sanitized.length() > 24) {
            sanitized = sanitized.substring(0, 24);
        }
        return sanitized.isBlank() ? "doc" : sanitized;
    }

    private Path resolveAndVerifyPath(String fileKey) {
        if (fileKey.contains("..") || fileKey.startsWith("/") || fileKey.startsWith("\\")) {
            throw new IllegalArgumentException("Invalid file key: directory traversal is prohibited");
        }
        Path resolved = this.rootPath.resolve(fileKey).normalize();
        if (!resolved.startsWith(this.rootPath)) {
            throw new IllegalArgumentException("Invalid file key: path traversal detected");
        }
        return resolved;
    }

    private String maskKeyForLog(String fileKey) {
        if (fileKey == null) {
            return "null";
        }
        String masked = AADHAAR_PATTERN.matcher(fileKey).replaceAll("[Aadhaar Redacted]");
        return PAN_PATTERN.matcher(masked).replaceAll("[PAN Redacted]");
    }
}
