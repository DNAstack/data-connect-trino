package com.dnastack.ga4gh.search.client.common;

import feign.Logger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SimpleLogger extends Logger {

    @Override
    protected void log(String configKey, String format, Object... args) {
        log.info("{} {}", configKey, String.format(format, args));
    }
}