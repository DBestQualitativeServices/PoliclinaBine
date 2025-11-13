package com.example.policlicabine.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "jwt")
@Validated
@Getter
@Setter
public class JwtProperties {

    @NotBlank(message = "JWT secret must not be blank")
    private String secret;

    @Min(value = 60000, message = "JWT expiration must be at least 60000 ms (1 minute)")
    private Long expiration = 1800000L; // 30 minutes

    @Min(value = 3600000, message = "JWT refresh expiration must be at least 3600000 ms (1 hour)")
    private Long refreshExpiration = 604800000L; // 7 days
}
