package com.dnastack.ga4gh.dataconnect;

import brave.Tracing;
import com.dnastack.auth.JwtTokenParser;
import com.dnastack.auth.JwtTokenParserFactory;
import com.dnastack.auth.PermissionChecker;
import com.dnastack.auth.PermissionCheckerFactory;
import com.dnastack.auth.client.TokenActionsHttpClientFactory;
import com.dnastack.auth.keyresolver.CachingIssuerPubKeyJwksResolver;
import com.dnastack.auth.keyresolver.IssuerPubKeyStaticResolver;
import com.dnastack.auth.model.IssuerInfo;
import com.dnastack.ga4gh.dataconnect.adapter.security.AuthConfig;
import com.dnastack.ga4gh.dataconnect.adapter.security.AuthConfig.OauthClientConfig;
import com.dnastack.ga4gh.dataconnect.adapter.security.DelegatingJwtDecoder;
import com.dnastack.ga4gh.dataconnect.adapter.security.ServiceAccountAuthenticator;
import com.dnastack.ga4gh.dataconnect.adapter.telemetry.TrinoTelemetryClient;
import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoClient;
import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoHttpClient;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@EnableWebSecurity
@Configuration
public class ApplicationConfig {

    private final String trinoDatasourceUrl;

    @Getter
    private final Set<String> hiddenCatalogs;


    /**
     * Other settings
     */
    private final String corsUrls;
    private final Converter<Jwt, ? extends AbstractAuthenticationToken> jwtScopesConverter;

    @Autowired
    public ApplicationConfig(
        Converter<Jwt, ? extends AbstractAuthenticationToken> jwtScopesConverter,
        @Value("${cors.urls}") String corsUrls,
        @Value("${trino.hidden-catalogs}") Set<String> hiddenCatalogs,
        @Value("${trino.datasource.url}") String trinoDatasourceUrl
    ) {
        this.jwtScopesConverter = jwtScopesConverter;
        this.corsUrls = corsUrls;
        this.hiddenCatalogs = hiddenCatalogs;
        this.trinoDatasourceUrl = trinoDatasourceUrl;
    }

    @Bean
    public ServiceAccountAuthenticator getServiceAccountAuthenticator(AuthConfig authConfig) {
        OauthClientConfig clientConfig = authConfig.getTrinoOauthClient();
        if (clientConfig == null) {
            return new ServiceAccountAuthenticator();
        } else {
            return new ServiceAccountAuthenticator(clientConfig);
        }
    }

    @Bean
    public OkHttpClient httpClient() {
        return new OkHttpClient();
    }

    @Bean
    public TrinoClient getTrinoClient(OkHttpClient httpClient, Tracing tracing, ServiceAccountAuthenticator accountAuthenticator, MeterRegistry registry) {
        return new TrinoTelemetryClient(
            new TrinoHttpClient(tracing, httpClient, trinoDatasourceUrl, accountAuthenticator), registry);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {

            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                    .allowedOrigins(parseCorsUrls())
                    .allowCredentials(true)
                    .allowedHeaders("*")
                    .allowedMethods("*");
            }
        };
    }

    @ConditionalOnExpression("'${app.auth.authorization-type}' == 'bearer' && '${app.auth.access-evaluator}' == 'scope'")
    @Configuration
    protected static class DefaultJwtSecurityConfig extends WebSecurityConfigurerAdapter {

        private final Converter<Jwt, ? extends AbstractAuthenticationToken> jwtScopesConverter;

        @Autowired
        public DefaultJwtSecurityConfig(Converter<Jwt, ? extends AbstractAuthenticationToken> jwtScopesConverter) {
            this.jwtScopesConverter = jwtScopesConverter;
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.cors().and()
                .headers()
                .frameOptions().deny()
                .xssProtection().and()
                .contentSecurityPolicy(
                    "default-src 'self'; " +
                    "style-src 'self'; " +
                    "font-src 'self'; " +
                    "script-src 'self';" +
                    "img-src 'self'; " +
                    "connect-src 'self';"
                ).and()
                .and()
                .authorizeRequests()
                .antMatchers("/actuator/health", "/actuator/info", "/service-info").permitAll()
                .antMatchers("/**")
                .authenticated()
                .and()
                .oauth2ResourceServer()
                .jwt().jwtAuthenticationConverter(jwtScopesConverter)
                .and()
                .and()
                .csrf()
                .disable();
        }

        @Bean
        public JwtDecoder jwtDecoder(AuthConfig authConfig) {
            List<AuthConfig.IssuerConfig> issuers = authConfig.getTokenIssuers();

            if (issuers == null || issuers.isEmpty()) {
                throw new IllegalArgumentException("At least one token issuer must be defined");
            }
            issuers = new ArrayList<>(issuers);
            return new DelegatingJwtDecoder(issuers);
        }

    }

    @ConditionalOnClass(name = { "com.dnastack.auth.PermissionChecker", "com.dnastack.auth.model.IssuerInfo" })
    @ConditionalOnExpression("'${app.auth.authorization-type}' == 'bearer' && '${app.auth.access-evaluator}' == 'wallet'")
    @Configuration
    protected static class WalletJwtSecurityConfig extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.cors().and()
                .headers()
                .frameOptions().deny()
                .xssProtection().and()
                .contentSecurityPolicy(
                    "default-src 'self'; " +
                    "style-src 'self'; " +
                    "font-src 'self'; " +
                    "script-src 'self';" +
                    "img-src 'self'; " +
                    "connect-src 'self';"
                ).and()
                .and()
                .authorizeRequests()
                .antMatchers("/actuator/health", "/actuator/info", "/service-info").permitAll()
                .antMatchers("/**")
                .authenticated()
                .and()
                .oauth2ResourceServer()
                .jwt()
                .and()
                .and()
                .csrf()
                .disable();
        }

        @Bean
        public List<IssuerInfo> allowedIssuers(AuthConfig authConfig) {
            List<AuthConfig.IssuerConfig> issuers = authConfig.getTokenIssuers();
            if (issuers == null || issuers.isEmpty()) {
                throw new IllegalArgumentException("At least one token issuer must be defined");
            }

            return authConfig.getTokenIssuers().stream()
                .map((issuerConfig) -> {
                    final String issuerUri = issuerConfig.getIssuerUri();
                    return IssuerInfo.IssuerInfoBuilder.builder()
                        .issuerUri(issuerUri)
                        .allowedAudiences(issuerConfig.getAudiences())
                        .publicKeyResolver(issuerConfig.getRsaPublicKey() != null
                            ? new IssuerPubKeyStaticResolver(issuerUri, issuerConfig.getRsaPublicKey())
                            : new CachingIssuerPubKeyJwksResolver(issuerUri))
                        .build();
                })
                .collect(Collectors.toUnmodifiableList());
        }

        @ConditionalOnExpression("'${app.auth.authorization-type}' == 'bearer'")
        @Bean
        public PermissionChecker permissionChecker(
            List<IssuerInfo> allowedIssuers,
            @Value("${app.url}") String policyEvaluationRequester,
            @Value("${app.auth.token-issuers[0].issuer-uri}") String walletUrl,
            Tracing tracing
        ) {
            String policyEvaluationUrl = stripTrailingSlashes(walletUrl) + "/policies/evaluations";
            return PermissionCheckerFactory.create(allowedIssuers, policyEvaluationRequester, policyEvaluationUrl, tracing);
        }

        private String stripTrailingSlashes(String url) {
            String slashlessUrl = url;
            while (slashlessUrl.endsWith("/")) {
                slashlessUrl = slashlessUrl.substring(0, slashlessUrl.length() - 1);
            }
            return slashlessUrl;
        }

        @Bean
        public JwtDecoder jwtDecoder(List<IssuerInfo> allowedIssuers, PermissionChecker permissionChecker, Tracing tracing) {
            final JwtTokenParser jwtTokenParser = JwtTokenParserFactory.create(allowedIssuers, TokenActionsHttpClientFactory.create(tracing));
            return (jwtToken) -> {
                try {
                    permissionChecker.checkPermissions(jwtToken);
                    final Jws<Claims> jws = jwtTokenParser.parseTokenClaims(jwtToken);
                    final JwsHeader headers = jws.getHeader();
                    final Claims claims = jws.getBody();
                    return new Jwt(jwtToken, claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant(), headers, claims);
                } catch (JwtException e) {
                    throw new org.springframework.security.oauth2.jwt.BadJwtException(e.getMessage(), e);
                }
            };
        }

    }

    @ConditionalOnExpression("'${app.auth.authorization-type}' == 'basic'")
    @Configuration
    protected static class BasicAuthSecurityConfig extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.cors().and()
                .headers()
                .frameOptions().deny()
                .xssProtection().and()
                .contentSecurityPolicy(
                    "default-src 'self'; " +
                    "style-src 'self'; " +
                    "font-src 'self'; " +
                    "script-src 'self';" +
                    "img-src 'self'; " +
                    "connect-src 'self';"
                ).and()
                .and()
                .authorizeRequests()
                .antMatchers("/api/**")
                .authenticated()
                .and()
                .httpBasic()
                .and()
                .authorizeRequests()
                .antMatchers("/actuator/health", "/actuator/info", "/service-info")
                .permitAll()
                .and()
                .authorizeRequests()
                .anyRequest()
                .authenticated()
                .and()
                .formLogin()
                .and()
                .csrf()
                .disable();
        }

    }

    @ConditionalOnExpression("'${app.auth.authorization-type}' == 'none'")
    @Configuration
    protected static class NoAuthSecurityConfig extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.cors().and().authorizeRequests().anyRequest().permitAll().and().csrf().disable();
        }

    }

    private String[] parseCorsUrls() {
        return corsUrls.split(",");
    }

}
