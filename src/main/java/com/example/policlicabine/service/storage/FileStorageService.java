package com.example.policlicabine.service.storage;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.entity.enums.FileCategory;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Interface for file storage operations.
 *
 * <p>This abstraction allows swapping storage implementations (local, Azure Blob Storage,
 * Cloudinary, etc.) without changing business logic code.
 *
 * <p>Implementations should be annotated with {@code @Service} and
 * {@code @ConditionalOnProperty} to enable profile-based switching.
 *
 * <h3>Available Implementations:</h3>
 * <ul>
 *   <li>{@code LocalFileStorageService} - Local filesystem storage</li>
 *   <li>{@code AzureBlobStorageService} - Azure Blob Storage (future)</li>
 *   <li>{@code CloudinaryStorageService} - Cloudinary CDN (future)</li>
 * </ul>
 *
 * @author PoliclicaBine System
 */
public interface FileStorageService {

    /**
     * Store file in the storage system and return metadata.
     *
     * @param file           the multipart file to store
     * @param category       the file category for organizing storage
     * @param uniqueFilename the unique filename to use for storage
     * @return Result containing StorageResult with file metadata, or failure message
     */
    Result<StorageResult> storeFile(
            MultipartFile file,
            FileCategory category,
            String uniqueFilename
    );

    /**
     * Load file as Spring Resource for download/streaming.
     *
     * @param storagePath the relative path in storage system
     * @return Result containing Resource, or failure if file not found
     */
    Result<Resource> loadFile(String storagePath);

    /**
     * Delete physical file from storage.
     *
     * @param storagePath the relative path in storage system
     * @return Result success or failure
     */
    Result<Void> deleteFile(String storagePath);

    /**
     * Check if file exists in storage.
     *
     * @param storagePath the relative path in storage system
     * @return true if file exists, false otherwise
     */
    boolean fileExists(String storagePath);

    /**
     * Get the storage type name for logging and monitoring.
     *
     * @return storage type (e.g., "LOCAL", "AZURE", "CLOUDINARY")
     */
    String getStorageType();
}
