package com.example.policlicabine.config;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.example.policlicabine.config.properties.AzureBlobStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        name = "file.storage.storage-type",
        havingValue = "AZURE"
)
@RequiredArgsConstructor
@Slf4j
public class AzureBlobStorageConfiguration {

    private final AzureBlobStorageProperties properties;

    @Bean
    public BlobServiceClient blobServiceClient() {
        log.info("Initializing Azure Blob Storage client with container: {}", properties.getContainerName());

        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .connectionString(properties.getConnectionString())
                .buildClient();

        BlobContainerClient containerClient = serviceClient.getBlobContainerClient(properties.getContainerName());
        if (!containerClient.exists()) {
            log.info("Container '{}' does not exist, creating...", properties.getContainerName());
            containerClient.create();
            log.info("Container '{}' created successfully", properties.getContainerName());
        } else {
            log.info("Container '{}' already exists", properties.getContainerName());
        }

        return serviceClient;
    }
}
