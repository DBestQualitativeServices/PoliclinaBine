package com.example.policlicabine.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Configuration properties for file storage system.
 *
 * <p>Configured via {@code file.storage.*} properties in application.properties.
 *
 * <h3>Example Configuration:</h3>
 * <pre>
 * file.storage.storage-type=LOCAL
 * file.storage.upload-dir=C:/uploads/policlicabine
 * file.storage.max-file-size=25MB
 * file.storage.allowed-mime-types=image/png,image/jpeg,image/jpg
 * file.storage.base-url=http://localhost:8080/api/files
 *
 * # Azure Blob Storage (future)
 * file.storage.azure.account-name=myaccount
 * file.storage.azure.container-name=files
 * </pre>
 *
 * @author PoliclicaBine System
 */
@ConfigurationProperties(prefix = "file.storage")
@Validated
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FileStorageProperties {

    /**
     * Directory path for local file storage
     */
    @NotBlank(message = "Upload directory must be specified")
    String uploadDir;

    /**
     * Maximum allowed file size (supports formats like "25MB", "1GB")
     */
    @NotNull
    @DataSizeUnit(DataUnit.MEGABYTES)
    DataSize maxFileSize = DataSize.ofMegabytes(25);

    /**
     * List of allowed MIME types for uploaded files
     */
    @NotEmpty
    List<String> allowedMimeTypes = List.of(
            "image/png",
            "image/jpeg",
            "image/jpg"
    );

    /**
     * Base URL for file download links
     */
    String baseUrl = "http://localhost:8080/api/files";

    /**
     * Storage provider type (LOCAL, AZURE, CLOUDINARY)
     */
    @NotNull
    StorageType storageType = StorageType.LOCAL;

    /**
     * Azure Blob Storage configuration (used when storageType=AZURE)
     */
    Azure azure = new Azure();

    /**
     * Cloudinary CDN configuration (used when storageType=CLOUDINARY)
     */
    Cloudinary cloudinary = new Cloudinary();

    /**
     * Azure Blob Storage configuration
     */
    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Azure {
        String accountName;
        String accountKey;
        String containerName;
        String endpoint;
    }

    /**
     * Cloudinary CDN configuration
     */
    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Cloudinary {
        String cloudName;
        String apiKey;
        String apiSecret;
    }

    /**
     * Supported storage provider types
     */
    public enum StorageType {
        /**
         * Local filesystem storage
         */
        LOCAL,

        /**
         * Azure Blob Storage
         */
        AZURE,

        /**
         * Cloudinary CDN
         */
        CLOUDINARY
    }
}
