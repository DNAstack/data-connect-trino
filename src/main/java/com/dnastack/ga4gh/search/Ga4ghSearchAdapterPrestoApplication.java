package com.dnastack.ga4gh.search;

import com.dnastack.audit.config.AuditEventLoggerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(AuditEventLoggerProperties.class) // can be removed on spring-boot version upgrade
public class Ga4ghSearchAdapterPrestoApplication {

    public static void main(String[] args) {
        SpringApplication.run(Ga4ghSearchAdapterPrestoApplication.class, args);
    }
}
