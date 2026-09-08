package com.dnastack.ga4gh.dataconnect.adapter.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.auth")
public class AuthConfig {


    OauthClientConfig trinoOauthClient = null;

    List<IssuerConfig> tokenIssuers = new ArrayList<>();


    @Getter
    @Setter
    public static class IssuerConfig {

        /**
         * The issuerUri this configuration applies to
         */
        String issuerUri;

        /**
         * The audience to test the JWT against. If this is set, the JWT must be issued for the appropriate audiences
         */
        List<String> audiences = new ArrayList<>();

        /**
         * The audiences a relayed {@code userToken} may be issued for. A caller sends that token in the
         * {@code GA4GH-Search-Authorization} header for the Trino plugins to evaluate data policy with, so it is
         * addressed to a downstream service rather than to this one and carries different audiences from the
         * bearer token. Deliberately separate from {@link #audiences}: were the two lists to overlap, a caller
         * could present its bearer token as a {@code userToken}, or the reverse. Empty means this service does
         * not hold a relayed token to the request's tenant.
         */
        List<String> userTokenAudiences = new ArrayList<>();

        /**
         * URI to fetch the JSON Web Key set from. This key set should provide the public keys to verify the supplied
         * bearer token
         */
        String jwkSetUri = null;

        /**
         * RSA public key to use to verify the JWTs.
         */
        String rsaPublicKey = null;


    }

    @Getter
    @Setter
    public static class OauthClientConfig {

        /**
         * The URI to use for authenticated service to service communication. This is expected to be an OIDC compliant
         * token endpoint which will accept {@code client_credentials}
         */
        String tokenUri;
        /**
         * The service account client id which will be used to authenticate this service against others
         */
        String clientId;
        /**
         * The service account client secret which will be used to authenticate this service against others
         */
        String clientSecret;

        /**
         * The scopes to request
         */
        String scopes = null;

        /**
         * The resource being requested
         */
        String resource = null;


        /**
         * The audience to request
         */
        String audience;
    }


}
