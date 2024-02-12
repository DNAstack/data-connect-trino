package com.dnastack.ga4gh.dataconnect.adapter.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@ConditionalOnExpression("${app.auth.global-method-security.enabled:true}")
@EnableMethodSecurity
public class GlobalMethodSecurityConfig {
}
