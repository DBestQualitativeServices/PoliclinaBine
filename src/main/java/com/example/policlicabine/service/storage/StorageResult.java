package com.example.policlicabine.service.storage;

import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
@With
public class StorageResult {

    /**
     * Relative path where file is stored (e.g., "consent_file/2024/01/file.pdf")
     */
    String storagePath;

    /**
     * Unique filename assigned during storage
     */
    String storedFilename;

    /**
     * File size in bytes
     */
    Long fileSize;

    /**
     * SHA-256 checksum hex string for file integrity verification
     */
    String checksum;
}
