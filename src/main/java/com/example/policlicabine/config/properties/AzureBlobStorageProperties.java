package com.example.policlicabine.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "file.storage.azure")
@Getter
@Setter
public class AzureBlobStorageProperties {

    private String connectionString;
    private String containerName = "files";
    private Integer sasExpiryMinutes = 60;
}
