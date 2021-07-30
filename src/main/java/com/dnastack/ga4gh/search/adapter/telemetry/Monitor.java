package com.dnastack.ga4gh.search.adapter.telemetry;

import io.micrometer.azuremonitor.AzureMonitorConfig;
import io.micrometer.azuremonitor.AzureMonitorMeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import io.micrometer.core.instrument.logging.LoggingRegistryConfig;
import io.micrometer.stackdriver.StackdriverConfig;
import io.micrometer.stackdriver.StackdriverMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class Monitor {

    private static MeterRegistry registry;
    private static final Map<String, Counter> counters = new HashMap<>();
    private static final Map<String, Timer> timers = new HashMap<>();
    private static boolean initialized = false;
    private static MonitorConfig config;

    //TODO: improve constructor
    public Monitor(MonitorConfig config) {
        Monitor.config = config;
    }

    public static Counter getCounter(String counterName) {
        return counters.getOrDefault(counterName, null);
    }

    //TODO: Synchronized here puts some restrictions on how this can be used.
    // Is this too lazy a mechanism to stop races when registering counters?
    public static synchronized Counter registerCounter(String counterName, String description, String... tags) {
        if (!initialized) {
            initialize();
        }

        if (counters.containsKey(counterName)) {
            throw new IllegalArgumentException("Counter " + counterName + " already exists.");
        }

        Counter counter = Counter
            .builder(counterName)
            .description(description)
            .tags(tags)
            .register(registry);
        counters.put(counterName, counter);
        log.info("Registered counter {}", counterName);
        return counter;
    }

    public static synchronized Timer registerRequestTimer(String timerName, String description, String... tags) {
        if (!initialized) {
            initialize();
        }

        if (timers.containsKey(timerName)) {
            throw new IllegalArgumentException("Timer " + timerName + " already exists.");
        }
        Timer timer = Timer
            .builder(timerName)
            .description(description)
            .tags(tags)
            .register(registry);
        timers.put(timerName, timer);
        log.info("Registered counter {}", timerName);
        return timer;
    }

    private static synchronized void initialize() {
        if (initialized) {
            return;
        }

        if (config.getStackDriver() != null) {
            StackdriverConfig stackdriverConfig = configureStackDriver(config.getStackDriver());
            registry = StackdriverMeterRegistry.builder(stackdriverConfig).build();
            initialized = true;
        }

        if (config.getAzureMonitor() != null) {
            AzureMonitorConfig azureMonitorConfig = configureAzureMonitor(config.getAzureMonitor());
            registry = AzureMonitorMeterRegistry.builder(azureMonitorConfig).build();
            initialized = true;

        }

        if (config.getLoggingMonitor() != null) {
            LoggingRegistryConfig loggingRegistryConfig = configureLoggingMonitor(config.getLoggingMonitor());
            registry = LoggingMeterRegistry.builder(loggingRegistryConfig).build();
            initialized = true;
        }

        if (!initialized) {
            log.warn("Attempted to initialize monitoring with no registry configured. " +
                "Defaulting to Logging monitor");
            MonitorConfig.LoggingMonitor loggingMonitorConfig = new MonitorConfig.LoggingMonitor();
            loggingMonitorConfig.setEnabled(true);
            loggingMonitorConfig.setStep(Duration.ofSeconds(30));
            LoggingRegistryConfig loggingRegistryConfig = configureLoggingMonitor(loggingMonitorConfig);
            registry = LoggingMeterRegistry.builder(loggingRegistryConfig).build();
            initialized = true;
            return;
        }

        if (config.getEnvironment() == null || config.getEnvironment().trim().isEmpty()) {
            throw new RuntimeException("Failed to initialize monitoring as environment was not populated.");
        }

        registry.config().commonTags("environment", config.getEnvironment());
        log.info("Finished setting up registry successfully.");
    }

    private static StackdriverConfig configureStackDriver(MonitorConfig.StackDriver config) {
        return new StackdriverConfig() {
            @Override
            public String get(String s) {
                return null;
            }

            @Override
            public String projectId() {
                return config.getProjectId();
            }

            @Override
            public Duration step() {
                return config.getStep();
            }
        };
    }


    private static AzureMonitorConfig configureAzureMonitor(MonitorConfig.AzureMonitor config) {
        return new AzureMonitorConfig() {
            @Override
            public String get(String s) {
                return null;
            }

            @Override
            public Duration step() {
                return config.getStep();
            }
        };
    }

    private static LoggingRegistryConfig configureLoggingMonitor(MonitorConfig.LoggingMonitor config) {
        return new LoggingRegistryConfig() {
            @Override
            public String get(String s) {
                return null;
            }

            @Override
            public Duration step() {
                return config.getStep();
            }
        };
    }
}
