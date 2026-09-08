package com.dnastack.ga4gh.dataconnect.adapter.security;

import com.dnastack.auth.PermissionChecker;
import com.dnastack.auth.exception.TenancyRequirementException;
import com.dnastack.ga4gh.dataconnect.adapter.trino.exception.RelayedTokenTenantMismatchException;
import com.dnastack.tenancy.context.TenantContextAccessor;
import com.dnastack.tenancy.context.TenantId;
import io.jsonwebtoken.JwtException;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import java.io.UncheckedIOException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * The request boundary for a caller's extra credentials: what it parses, and when it holds the relayed
 * {@code userToken} to the tenant of the request carrying it.
 */
public class ClientSuppliedCredentialsTest {

    private final TenantContextAccessor tenantContextAccessor = new TenantContextAccessor();
    private final PermissionChecker userTokenPermissionChecker = mock(PermissionChecker.class);

    private ClientSuppliedCredentials credentialsReader() {
        return new ClientSuppliedCredentials(tenantContextAccessor,
            Optional.of(new UserTokenTenancyValidator(userTokenPermissionChecker)));
    }

    @Test
    public void parse_should_returnEachCredential_when_theHeaderCarriesSeveral() {
        Map<String, String> credentials = credentialsReader()
            .parse(List.of("userToken=a.b.c", "somethingElse=with=an=equals"));

        assertThat(credentials)
            .as("the credentials read from the header")
            .containsEntry("userToken", "a.b.c")
            .containsEntry("somethingElse", "with=an=equals");
    }

    @Test
    public void parse_should_checkTheUserTokenAgainstTheRequestTenant() {
        UUID requestTenant = UUID.randomUUID();

        tenantContextAccessor.runAs(requestTenant, () -> credentialsReader().parse(List.of("userToken=a.b.c")));

        verify(userTokenPermissionChecker).checkTokenTenancy("a.b.c", requestTenant);
    }

    @Test
    public void parse_should_checkAgainstTheManagementTenant_when_theRequestNamedNoTenant() {
        credentialsReader().parse(List.of("userToken=a.b.c"));

        verify(userTokenPermissionChecker).checkTokenTenancy("a.b.c", TenantId.MANAGEMENT.getValue());
    }

    @Test
    public void parse_should_checkNothing_when_theCallerSuppliedNoUserToken() {
        credentialsReader().parse(List.of("somethingElse=value"));

        verify(userTokenPermissionChecker, never()).checkTokenTenancy(any(), any());
    }

    @Test
    public void parse_should_checkNothing_when_noValidatorIsConfigured() {
        // The bearer profiles host the validator; under basic and no-auth there is none to hold a token to.
        Map<String, String> credentials =
            new ClientSuppliedCredentials(tenantContextAccessor, Optional.empty())
                .parse(List.of("userToken=a.b.c"));

        assertThat(credentials).as("the credentials read without a validator").containsEntry("userToken", "a.b.c");
        verify(userTokenPermissionChecker, never()).checkTokenTenancy(any(), any());
    }

    @Test
    public void parse_should_throwForbidden_when_theUserTokenNamesAnotherTenant() {
        UUID requestTenant = UUID.randomUUID();
        doThrow(new TenancyRequirementException("token tenant [x] does not match resource tenant [y]"))
            .when(userTokenPermissionChecker).checkTokenTenancy(any(), eq(requestTenant));

        assertThatThrownBy(() -> tenantContextAccessor.runAs(requestTenant,
            () -> credentialsReader().parse(List.of("userToken=a.b.c"))))
            .isInstanceOf(RelayedTokenTenantMismatchException.class)
            .hasMessageContaining("not issued for the tenant this request addresses")
            .extracting(e -> ((RelayedTokenTenantMismatchException) e).httpStatus())
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void parse_should_throwForbidden_when_theUserTokenDoesNotVerify() {
        doThrow(new JwtException("bad signature")).when(userTokenPermissionChecker).checkTokenTenancy(any(), any());

        assertThatThrownBy(() -> credentialsReader().parse(List.of("userToken=a.b.c")))
            .isInstanceOf(RelayedTokenTenantMismatchException.class)
            .hasMessageContaining("could not be verified");
    }

    @Test
    public void parse_should_notBlameTheCaller_when_theIssuersKeysAreUnreachable() {
        // Failing to reach wallet is this service's problem, and answering 403 would send the caller looking
        // for a permission it already holds.
        doThrow(new UncheckedIOException(new IOException("jwks unreachable")))
            .when(userTokenPermissionChecker).checkTokenTenancy(any(), any());

        assertThatThrownBy(() -> credentialsReader().parse(List.of("userToken=a.b.c")))
            .isInstanceOf(UncheckedIOException.class);
    }
}
