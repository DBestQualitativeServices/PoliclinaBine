package com.example.policlicabine.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class InitialConfig {

    @Bean
    CommandLineRunner commandLineRunner(
    ) {
        return args -> {
        };
    }
}
