package com.dnastack.ga4gh.dataconnect;

import com.dnastack.audit.config.AuditEventLoggerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(AuditEventLoggerProperties.class) // can be removed on spring-boot version upgrade
public class DataConnectTrinoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataConnectTrinoApplication.class, args);
    }
}
