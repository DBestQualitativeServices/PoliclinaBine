package com.example.policlicabine;

import com.microsoft.applicationinsights.attach.ApplicationInsights;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.xml.transform.Result;

@SpringBootApplication
@EnableScheduling
public class PoliclicaBineApplication {

    public static void main(String[] args) {
        // Enable Azure Application Insights monitoring
        ApplicationInsights.attach();

        SpringApplication.run(PoliclicaBineApplication.class, args);
    }

}
