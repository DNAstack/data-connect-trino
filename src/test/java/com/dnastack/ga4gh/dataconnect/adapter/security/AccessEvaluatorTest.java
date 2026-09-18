package com.dnastack.ga4gh.dataconnect.adapter.security;

import com.dnastack.auth.PermissionChecker;
import com.dnastack.tenancy.context.TenantContextAccessor;
import com.dnastack.tenancy.context.TenantId;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * What every {@code @PreAuthorize} on this service resolves to. The decision itself belongs to the token
 * validator; these tests check that the tenant the request addresses is the tenant the caller's token is held
 * to, and that this service denies a request with nothing to judge rather than allowing it.
 */
public class AccessEvaluatorTest {

    private static final String APP_URL = "https://data-connect.example.com";
    private static final String CALLERS_TOKEN = "the.callers.token";
    private static final Set<String> ACTIONS = Set.of("data-connect:query");
    private static final Set<String> SCOPES = Set.of("data-connect:query");

    private final PermissionChecker permissionChecker = mock(PermissionChecker.class);
    private final TenantContextAccessor tenantContextAccessor = new TenantContextAccessor();

    private AccessEvaluator accessEvaluator() {
        AccessEvaluator accessEvaluator = new AccessEvaluator();
        ReflectionTestUtils.setField(accessEvaluator, "appUrl", APP_URL);
        ReflectionTestUtils.setField(accessEvaluator, "tenantContextAccessor", tenantContextAccessor);
        ReflectionTestUtils.setField(accessEvaluator, "accessEvaluatorMethod",
            accessEvaluator.walletAccessEvaluator(permissionChecker));
        return accessEvaluator;
    }

    /** A caller authenticated by a bearer token, as the filter chain leaves the context. */
    private static void authenticateWithBearerToken() {
        Jwt jwt = Jwt.withTokenValue(CALLERS_TOKEN)
            .header("alg", "RS256")
            .claim("sub", "a-caller")
            .build();
        SecurityContextHolder.getContext()
            .setAuthentication(new TestingAuthenticationToken(jwt, CALLERS_TOKEN, "SCOPE_data-connect:query"));
    }

    @After
    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private UUID tenantPassedToTheChecker() {
        ArgumentCaptor<UUID> tenantId = ArgumentCaptor.forClass(UUID.class);
        verify(permissionChecker)
            .hasPermissions(anyString(), tenantId.capture(), anySet(), anyString(), anySet());
        return tenantId.getValue();
    }

    @Test
    public void canAccessTenantResource_should_holdTheTokenToTheTenantTheRequestAddresses() {
        UUID requestTenant = UUID.randomUUID();
        authenticateWithBearerToken();
        AccessEvaluator accessEvaluator = accessEvaluator();

        tenantContextAccessor.runAs(requestTenant,
            () -> accessEvaluator.canAccessTenantResource("/search", ACTIONS, SCOPES));

        assertThat(tenantPassedToTheChecker())
            .as("the tenant a request addressing one is judged against")
            .isEqualTo(requestTenant);
    }

    @Test
    public void canAccessTenantResource_should_holdTheTokenToTheManagementTenant_when_theRequestNamedNoTenant() {
        // A legacy un-prefixed path resolves to the management tenant, which is where its data has always lived.
        authenticateWithBearerToken();

        accessEvaluator().canAccessTenantResource("/search", ACTIONS, SCOPES);

        assertThat(tenantPassedToTheChecker())
            .as("the tenant a legacy request is judged against")
            .isEqualTo(TenantId.MANAGEMENT.getValue());
    }

    @Test
    public void canAccessTenantResource_should_askAboutAResourceUrlCarryingNoTenant() {
        // One template-group policy covers every tenant because isolation comes from the tenant match, not the
        // resource URI. Were the tenant in the URI, every tenant would need its own policy statement.
        UUID requestTenant = UUID.randomUUID();
        authenticateWithBearerToken();
        AccessEvaluator accessEvaluator = accessEvaluator();

        tenantContextAccessor.runAs(requestTenant,
            () -> accessEvaluator.canAccessTenantResource("/search", ACTIONS, SCOPES));

        ArgumentCaptor<String> resourceUrl = ArgumentCaptor.forClass(String.class);
        verify(permissionChecker)
            .hasPermissions(anyString(), any(), anySet(), resourceUrl.capture(), anySet());
        assertThat(resourceUrl.getValue())
            .as("the resource URL the policy is asked about")
            .isEqualTo(APP_URL + "/search");
    }

    @Test
    public void canAccessTenantResource_should_passTheCallersOwnToken() {
        authenticateWithBearerToken();
        AccessEvaluator accessEvaluator = accessEvaluator();

        accessEvaluator.canAccessTenantResource("/search", ACTIONS, SCOPES);

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(permissionChecker).hasPermissions(token.capture(), any(), anySet(), anyString(), anySet());
        assertThat(token.getValue()).as("the token the policy is evaluated for").isEqualTo(CALLERS_TOKEN);
    }

    @Test
    public void canAccessTenantResource_should_grantAccess_when_theTokenHoldsThePermissions() {
        authenticateWithBearerToken();
        when(permissionChecker.hasPermissions(anyString(), any(), anySet(), anyString(), anySet()))
            .thenReturn(true);

        assertThat(accessEvaluator().canAccessTenantResource("/search", ACTIONS, SCOPES))
            .as("access for a token holding the required permissions")
            .isTrue();
    }

    @Test
    public void canAccessTenantResource_should_denyAccess_when_theTokenDoesNotHoldThePermissions() {
        authenticateWithBearerToken();
        when(permissionChecker.hasPermissions(anyString(), any(), anySet(), anyString(), anySet()))
            .thenReturn(false);

        assertThat(accessEvaluator().canAccessTenantResource("/search", ACTIONS, SCOPES))
            .as("access for a token that does not hold the required permissions")
            .isFalse();
    }

    @Test
    public void canAccessTenantResource_should_denyAccess_when_theContextHoldsNoAuthentication() {
        SecurityContextHolder.clearContext();

        assertThat(accessEvaluator().canAccessTenantResource("/search", ACTIONS, SCOPES))
            .as("access with no authentication in the context")
            .isFalse();
    }

    @Test
    public void canAccessTenantResource_should_askThePolicyNothing_when_theContextHoldsNoAuthentication() {
        SecurityContextHolder.clearContext();

        accessEvaluator().canAccessTenantResource("/search", ACTIONS, SCOPES);

        verify(permissionChecker, never()).hasPermissions(anyString(), any(), anySet(), anyString(), anySet());
    }

    @Test
    public void canAccessTenantResource_should_denyAccess_when_thePrincipalIsNotABearerToken() {
        SecurityContextHolder.getContext()
            .setAuthentication(new TestingAuthenticationToken("a-username", "a-password"));

        assertThat(accessEvaluator().canAccessTenantResource("/search", ACTIONS, SCOPES))
            .as("access for a principal that is not a bearer token")
            .isFalse();
    }
}
