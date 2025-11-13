package com.example.policlicabine.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "swagger")
@Getter
@Setter
public class SwaggerProperties {

    private boolean enabled = true;
}
