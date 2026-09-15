package com.dnastack.ga4gh.dataconnect.adapter.security;

import com.dnastack.auth.PermissionChecker;
import com.dnastack.auth.PermissionCheckerFactory;
import com.dnastack.auth.exception.TenancyRequirementException;
import com.dnastack.auth.model.IssuerInfo;
import com.dnastack.auth.model.TenancyEnforcement;
import com.dnastack.ga4gh.dataconnect.adapter.trino.exception.RelayedTokenTenantMismatchException;
import com.dnastack.tenancy.context.TenantId;
import io.jsonwebtoken.JwtException;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;

import java.util.Collection;
import java.util.List;

/**
 * Holds a relayed {@code userToken} to the tenant of the request carrying it.
 * <p>
 * The token is the caller's own, sent in the {@code GA4GH-Search-Authorization} header for the Trino plugins to
 * evaluate data policy with. Its scopes and actions describe the resources of the service it is addressed to, so
 * this service cannot make an authorization decision from it — but it can insist that the tenant it was issued
 * for is the tenant the request addresses, which is what stops a caller pairing one tenant's path with another
 * tenant's data authority.
 * <p>
 * The tenancy ladder decides what a disagreement costs: {@code LOG_ONLY} warns and allows,
 * {@code ACCEPT_TENANTLESS} and {@code REQUIRE_TENANT} deny. That ladder lives in the token validator, so this
 * class chooses nothing and only translates a denial into the service's own error.
 */
@Slf4j
public class UserTokenTenancyValidator {

    /** Absent where this deployment holds no relayed token to a tenant. See {@link #checkingNothing()}. */
    private final PermissionChecker userTokenPermissionChecker;

    UserTokenTenancyValidator(PermissionChecker userTokenPermissionChecker) {
        this.userTokenPermissionChecker = userTokenPermissionChecker;
    }

    /**
     * A validator for a deployment with nothing to check: one whose authentication is not bearer tokens at all,
     * or one that has named no audience for a relayed token. Callers hold a validator either way, so "there is
     * nothing to check here" is stated once, here, rather than at every place one is used.
     */
    public static UserTokenTenancyValidator checkingNothing() {
        return new UserTokenTenancyValidator(null);
    }

    /**
     * Builds a validator over the same issuers as the bearer token, expecting the audiences a relayed token
     * carries instead of this service's own.
     *
     * @return a validator that checks nothing when no issuer declares a relayed-token audience, which is how a
     * deployment that has not configured one keeps working. Which of the two it is, is logged at startup.
     */
    public static UserTokenTenancyValidator create(
        List<AuthConfig.IssuerConfig> issuerConfigs,
        List<IssuerInfo> allowedIssuers,
        String policyEvaluationRequester,
        String policyEvaluationUrl,
        TenancyEnforcement tenancyEnforcement,
        ObservationRegistry observationRegistry,
        ConnectionPool connectionPool
    ) {
        List<IssuerInfo> userTokenIssuers = allowedIssuers.stream()
            .map(issuer -> IssuerInfo.IssuerInfoBuilder.builder()
                .issuerUri(issuer.getIssuerUri())
                .allowedAudiences(userTokenAudiencesOf(issuerConfigs, issuer.getIssuerUri()))
                .allowedResources(issuer.getAllowedResources())
                .publicKeyResolver(issuer.getPublicKeyResolver())
                .build())
            .filter(issuer -> !issuer.getAllowedAudiences().isEmpty())
            .toList();

        if (userTokenIssuers.isEmpty()) {
            log.info("No app.auth.token-issuers[].user-token-audiences are configured, so a relayed userToken "
                + "will not be held to the tenant of the request carrying it");
            return checkingNothing();
        }

        log.info("A relayed userToken will be held to the tenant of the request carrying it, accepting the "
            + "audiences {} under enforcement {}", userTokenIssuers.stream().map(IssuerInfo::getAllowedAudiences)
            .toList(), tenancyEnforcement);
        return new UserTokenTenancyValidator(PermissionCheckerFactory.create(userTokenIssuers,
            policyEvaluationRequester, policyEvaluationUrl, observationRegistry, connectionPool, tenancyEnforcement));
    }

    private static Collection<String> userTokenAudiencesOf(List<AuthConfig.IssuerConfig> issuerConfigs, String issuerUri) {
        return issuerConfigs.stream()
            .filter(config -> issuerUri.equals(config.getIssuerUri()))
            .findFirst()
            .map(AuthConfig.IssuerConfig::getUserTokenAudiences)
            .map(audiences -> (Collection<String>) audiences)
            .orElse(List.of());
    }

    /**
     * @param requestTenant the tenant the request addresses
     * @param userToken the token the caller supplied for relaying
     * @throws RelayedTokenTenantMismatchException if the token was issued for another tenant, or does not
     * verify against a configured issuer and relayed-token audience — under every enforcement mode but
     * {@code LOG_ONLY}. A failure to reach the issuer's keys is not translated: that is this service's
     * problem rather than the caller's, and it surfaces as such.
     */
    public void validate(TenantId requestTenant, String userToken) {
        if (userTokenPermissionChecker == null) {
            return;
        }
        try {
            userTokenPermissionChecker.checkTokenTenancy(userToken, requestTenant.getValue());
        } catch (TenancyRequirementException e) {
            throw new RelayedTokenTenantMismatchException(
                "The supplied userToken is not issued for the tenant this request addresses", e);
        } catch (JwtException | IllegalArgumentException e) {
            throw new RelayedTokenTenantMismatchException("The supplied userToken could not be verified", e);
        }
    }
}
