package com.example.policlicabine.service.storage;

import com.example.policlicabine.config.properties.FileStorageProperties;
import com.example.policlicabine.entity.enums.FileCategory;
import com.example.policlicabine.exception.FileStorageException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Local filesystem implementation of file storage.
 *
 * <p>Features:
 * <ul>
 *   <li>Stores files in local filesystem with category subdirectories</li>
 *   <li>Automatic retry on I/O failures (up to 3 attempts)</li>
 *   <li>SHA-256 checksum calculation for integrity</li>
 *   <li>Directory auto-creation on startup</li>
 *   <li>Thread-safe file operations</li>
 * </ul>
 *
 * <p>Activated when {@code file.storage.storage-type=LOCAL} (default)
 *
 * @author PoliclicaBine System
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "file.storage.storage-type",
        havingValue = "LOCAL",
        matchIfMissing = true
)
public class LocalFileStorageService implements FileStorageService {

    private final FileStorageProperties properties;

    /**
     * Initialize storage directory on application startup
     */
    @PostConstruct
    public void init() {
        try {
            Path uploadPath = Paths.get(properties.getUploadDir());
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", uploadPath.toAbsolutePath());
            } else {
                log.info("Upload directory already exists: {}", uploadPath.toAbsolutePath());
            }

            // Verify directory is writable
            if (!Files.isWritable(uploadPath)) {
                throw new FileStorageException("Upload directory is not writable: " + uploadPath);
            }

        } catch (IOException e) {
            throw new FileStorageException("Could not create upload directory", e);
        }
    }

    @Override
    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2),
            retryFor = {IOException.class}
    )
    public StorageResult storeFile(
            MultipartFile file,
            FileCategory category,
            String uniqueFilename
    ) {
        try {
            // Create category subdirectory (e.g., "consent_file/")
            Path categoryPath = Paths.get(properties.getUploadDir(),
                    category.name().toLowerCase());

            if (!Files.exists(categoryPath)) {
                Files.createDirectories(categoryPath);
                log.debug("Created category directory: {}", categoryPath);
            }

            // Determine target file path
            Path targetPath = categoryPath.resolve(uniqueFilename);

            // Copy file content using Spring's StreamUtils for efficiency
            try (InputStream inputStream = file.getInputStream();
                 OutputStream outputStream = Files.newOutputStream(targetPath)) {

                StreamUtils.copy(inputStream, outputStream);
            }

            log.info("Stored file: {} ({} bytes) in category: {}",
                    uniqueFilename, file.getSize(), category);

            // Calculate checksum for integrity verification
            String checksum = calculateChecksum(targetPath);

            // Build relative storage path
            String relativePath = category.name().toLowerCase() + "/" + uniqueFilename;

            // Return result
            return StorageResult.builder()
                    .storagePath(relativePath)
                    .storedFilename(uniqueFilename)
                    .fileSize(file.getSize())
                    .checksum(checksum)
                    .build();

        } catch (IOException e) {
            log.error("Failed to store file: {} in category: {}", uniqueFilename, category, e);
            throw new FileStorageException("Failed to store file: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback method when all retry attempts fail
     */
    @Recover
    public StorageResult recoverStoreFile(
            IOException e,
            MultipartFile file,
            FileCategory category,
            String uniqueFilename
    ) {
        log.error("Failed to store file after {} retries: {}", 3, uniqueFilename, e);
        throw new FileStorageException("Failed to store file after multiple attempts: " + e.getMessage(), e);
    }

    @Override
    public Resource loadFile(String storagePath) {
        try {
            Path filePath = Paths.get(properties.getUploadDir()).resolve(storagePath).normalize();

            // Security check: prevent path traversal attacks
            if (!filePath.startsWith(Paths.get(properties.getUploadDir()))) {
                log.warn("Path traversal attempt detected: {}", storagePath);
                throw new FileStorageException("Invalid file path");
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                log.debug("Loaded file: {}", storagePath);
                return resource;
            } else {
                log.warn("File not found or not readable: {}", storagePath);
                throw new ResourceNotFoundException("File", storagePath);
            }

        } catch (MalformedURLException e) {
            log.error("Invalid file path: {}", storagePath, e);
            throw new FileStorageException("Invalid file path: " + storagePath, e);
        }
    }

    @Override
    @Retryable(
            maxAttempts = 2,
            backoff = @Backoff(delay = 500),
            retryFor = {IOException.class}
    )
    public void deleteFile(String storagePath) {
        try {
            Path filePath = Paths.get(properties.getUploadDir()).resolve(storagePath).normalize();

            // Security check: prevent path traversal
            if (!filePath.startsWith(Paths.get(properties.getUploadDir()))) {
                log.warn("Path traversal attempt in delete: {}", storagePath);
                throw new FileStorageException("Invalid file path");
            }

            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("Deleted file: {}", storagePath);
            } else {
                log.debug("File already deleted: {}", storagePath);
            }

        } catch (IOException e) {
            log.error("Failed to delete file: {}", storagePath, e);
            throw new FileStorageException("Failed to delete file: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean fileExists(String storagePath) {
        try {
            Path filePath = Paths.get(properties.getUploadDir()).resolve(storagePath).normalize();

            // Security check
            if (!filePath.startsWith(Paths.get(properties.getUploadDir()))) {
                return false;
            }

            return Files.exists(filePath);

        } catch (Exception e) {
            log.error("Error checking file existence: {}", storagePath, e);
            return false;
        }
    }

    @Override
    public String getStorageType() {
        return "LOCAL";
    }

    /**
     * Calculate SHA-256 checksum of file for integrity verification
     *
     * @param filePath path to file
     * @return hex-encoded checksum string
     * @throws IOException if file cannot be read
     */
    private String calculateChecksum(Path filePath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            try (InputStream fis = Files.newInputStream(filePath)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }

            byte[] hash = digest.digest();
            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            // SHA-256 should always be available
            throw new FileStorageException("SHA-256 algorithm not available", e);
        }
    }
}
