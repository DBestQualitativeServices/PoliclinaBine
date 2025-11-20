package com.example.policlicabine.service.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.example.policlicabine.common.Result;
import com.example.policlicabine.config.properties.AzureBlobStorageProperties;
import com.example.policlicabine.entity.enums.FileCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "file.storage.storage-type",
        havingValue = "AZURE"
)
public class AzureBlobStorageService implements FileStorageService {

    private final BlobServiceClient blobServiceClient;
    private final AzureBlobStorageProperties properties;

    @Override
    public Result<StorageResult> storeFile(
            MultipartFile file,
            FileCategory category,
            String uniqueFilename
    ) {
        if (file == null || file.isEmpty()) {
            return Result.failure("File is empty or null");
        }

        try {
            String blobPath = buildBlobPath(category, uniqueFilename);
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(properties.getContainerName());
            BlobClient blobClient = containerClient.getBlobClient(blobPath);

            try (InputStream inputStream = file.getInputStream()) {
                String checksum = calculateChecksum(inputStream);

                inputStream.reset();
                blobClient.upload(inputStream, file.getSize(), true);

                log.info("File uploaded successfully to Azure Blob Storage: {}", blobPath);

                StorageResult result = StorageResult.builder()
                        .storagePath(blobPath)
                        .storedFilename(uniqueFilename)
                        .fileSize(file.getSize())
                        .checksum(checksum)
                        .build();

                return Result.success(result);

            }
        } catch (BlobStorageException e) {
            log.error("Azure Blob Storage error during upload: {}", e.getMessage(), e);
            return Result.failure("Failed to upload file to Azure: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during file upload: {}", e.getMessage(), e);
            return Result.failure("Failed to upload file: " + e.getMessage());
        }
    }

    @Override
    public Result<Resource> loadFile(String storagePath) {
        if (storagePath == null || storagePath.trim().isEmpty()) {
            return Result.failure("Storage path is required");
        }

        try {
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(properties.getContainerName());
            BlobClient blobClient = containerClient.getBlobClient(storagePath);

            if (!blobClient.exists()) {
                return Result.failure("File not found: " + storagePath);
            }

            String sasUrl = generateSasUrl(blobClient);
            Resource resource = new UrlResource(sasUrl);

            log.info("Generated SAS URL for file: {}", storagePath);
            return Result.success(resource);

        } catch (MalformedURLException e) {
            log.error("Invalid SAS URL generated: {}", e.getMessage(), e);
            return Result.failure("Failed to generate download URL: " + e.getMessage());
        } catch (BlobStorageException e) {
            log.error("Azure Blob Storage error during file load: {}", e.getMessage(), e);
            return Result.failure("Failed to load file from Azure: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during file load: {}", e.getMessage(), e);
            return Result.failure("Failed to load file: " + e.getMessage());
        }
    }

    @Override
    public Result<Void> deleteFile(String storagePath) {
        if (storagePath == null || storagePath.trim().isEmpty()) {
            return Result.failure("Storage path is required");
        }

        try {
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(properties.getContainerName());
            BlobClient blobClient = containerClient.getBlobClient(storagePath);

            boolean deleted = blobClient.deleteIfExists();

            if (deleted) {
                log.info("File deleted successfully from Azure: {}", storagePath);
                return Result.success(null);
            } else {
                log.warn("File not found for deletion: {}", storagePath);
                return Result.failure("File not found: " + storagePath);
            }

        } catch (BlobStorageException e) {
            log.error("Azure Blob Storage error during deletion: {}", e.getMessage(), e);
            return Result.failure("Failed to delete file from Azure: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during file deletion: {}", e.getMessage(), e);
            return Result.failure("Failed to delete file: " + e.getMessage());
        }
    }

    @Override
    public boolean fileExists(String storagePath) {
        if (storagePath == null || storagePath.trim().isEmpty()) {
            return false;
        }

        try {
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(properties.getContainerName());
            BlobClient blobClient = containerClient.getBlobClient(storagePath);
            return blobClient.exists();

        } catch (BlobStorageException e) {
            log.error("Azure Blob Storage error checking file existence: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error checking file existence: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getStorageType() {
        return "AZURE";
    }

    private String buildBlobPath(FileCategory category, String filename) {
        return category.name().toLowerCase().replace('_', '-') + "/" + filename;
    }

    private String generateSasUrl(BlobClient blobClient) {
        OffsetDateTime expiryTime = OffsetDateTime.now().plusMinutes(properties.getSasExpiryMinutes());

        BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);
        BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(expiryTime, permission);

        String sasToken = blobClient.generateSas(sasValues);
        return blobClient.getBlobUrl() + "?" + sasToken;
    }

    private String calculateChecksum(InputStream inputStream) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            digest.update(buffer, 0, bytesRead);
        }

        byte[] hashBytes = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }
}
