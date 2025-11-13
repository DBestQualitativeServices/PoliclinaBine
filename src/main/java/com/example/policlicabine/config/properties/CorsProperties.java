package com.example.policlicabine.config.properties;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "cors")
@Validated
@Getter
@Setter
public class CorsProperties {

    @NotEmpty(message = "CORS allowed origins must not be empty")
    private String allowedOrigins = "http://localhost:5173";

    private String allowedMethods = "GET,POST,PUT,DELETE,PATCH,OPTIONS";

    private String allowedHeaders = "Authorization,Content-Type,Accept,Origin,X-Requested-With";

    private String exposedHeaders = "Authorization";

    private Boolean allowCredentials = true;

    private Long maxAge = 3600L;

    public String[] getAllowedOriginsArray() {
        return allowedOrigins.split(",");
    }

    public String[] getAllowedMethodsArray() {
        return allowedMethods.split(",");
    }

    public String[] getAllowedHeadersArray() {
        return allowedHeaders.split(",");
    }

    public String[] getExposedHeadersArray() {
        return exposedHeaders.split(",");
    }
}
