package com.example.policlicabine;

import com.microsoft.applicationinsights.attach.ApplicationInsights;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableRetry
@EnableAsync
@ConfigurationPropertiesScan("com.example.policlicabine.config.properties")
public class PoliclicaBineApplication {

    public static void main(String[] args) {
        // Enable Azure Application Insights monitoring
        ApplicationInsights.attach();

        SpringApplication.run(PoliclicaBineApplication.class, args);
    }

}
