package com.example.policlicabine.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

/**
 * Spring Data Web configuration for pagination support.
 *
 * <p>Enables stable JSON serialization for Page objects using PagedModel DTOs
 * instead of internal PageImpl structure. This ensures the JSON API contract
 * remains stable across Spring Data version upgrades.
 *
 * <p>Configuration applies globally to all REST controllers returning Page<?> objects.
 *
 * @see EnableSpringDataWebSupport
 * @see org.springframework.data.web.PagedModel
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class SpringDataWebConfig {
    // No additional configuration needed - annotation handles everything
}
