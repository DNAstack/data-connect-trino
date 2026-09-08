package com.dnastack.ga4gh.dataconnect.adapter.security;

import com.dnastack.tenancy.context.TenantContextAccessor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Reads the extra credentials a caller sends in the {@code GA4GH-Search-Authorization} header, holding the
 * {@code userToken} among them to the tenant of the request that carried it.
 * <p>
 * This is the request boundary for those credentials: every handler that accepts the header reads it through
 * here, so a token issued for another tenant is refused before any query is submitted to Trino rather than
 * after Trino has begun work on it.
 */
@Component
public class ClientSuppliedCredentials {

    private static final String USER_TOKEN_CREDENTIAL = "userToken";

    private final TenantContextAccessor tenantContextAccessor;
    private final Optional<UserTokenTenancyValidator> userTokenTenancyValidator;

    public ClientSuppliedCredentials(
        TenantContextAccessor tenantContextAccessor,
        Optional<UserTokenTenancyValidator> userTokenTenancyValidator
    ) {
        this.tenantContextAccessor = tenantContextAccessor;
        this.userTokenTenancyValidator = userTokenTenancyValidator;
    }

    /**
     * @param clientSuppliedCredentials the header's values, each a {@code name=value} pair
     * @return the credentials by name
     * @throws com.dnastack.ga4gh.dataconnect.adapter.trino.exception.RelayedTokenTenantMismatchException if a
     * supplied {@code userToken} names a tenant other than the request's
     */
    public Map<String, String> parse(List<String> clientSuppliedCredentials) {
        Map<String, String> credentials = clientSuppliedCredentials.stream()
            .map(val -> val.split("=", 2))
            .collect(Collectors.toMap(kv -> kv[0], kv -> kv[1]));

        String userToken = credentials.get(USER_TOKEN_CREDENTIAL);
        if (userToken != null) {
            userTokenTenancyValidator.ifPresent(validator ->
                validator.validate(tenantContextAccessor.getTenantId(), userToken));
        }

        return credentials;
    }
}
