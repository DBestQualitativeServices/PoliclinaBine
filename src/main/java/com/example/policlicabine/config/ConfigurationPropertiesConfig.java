package com.example.policlicabine.config;

import com.example.policlicabine.config.properties.CorsProperties;
import com.example.policlicabine.config.properties.JwtProperties;
import com.example.policlicabine.config.properties.SwaggerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    JwtProperties.class,
    CorsProperties.class,
    SwaggerProperties.class
})
public class ConfigurationPropertiesConfig {
    // No additional configuration needed
    // @EnableConfigurationProperties automatically registers the specified classes as beans
}
