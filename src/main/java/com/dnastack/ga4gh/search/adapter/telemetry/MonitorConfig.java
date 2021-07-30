package com.dnastack.ga4gh.search.adapter.telemetry;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "management.metrics.export")
public class MonitorConfig {

    String environment = null;
    StackDriver stackDriver = null;
    AzureMonitor azureMonitor = null;
    LoggingMonitor loggingMonitor = null;

    @Getter
    @Setter
    public static class StackDriver {
        String projectId;
        Duration step;
    }

    @Getter
    @Setter
    public static class AzureMonitor {
        boolean enabled;
        Duration step;
    }

    @Getter
    @Setter
    public static class LoggingMonitor {
        boolean enabled;
        Duration step = Duration.ofSeconds(30);
    }
}
