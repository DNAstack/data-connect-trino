package com.dnastack.ga4gh.search.adapter.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

@Configuration
@ConditionalOnExpression("${app.auth.global-method-security.enabled:true}")
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class GlobalMethodSecurityConfig {
}
