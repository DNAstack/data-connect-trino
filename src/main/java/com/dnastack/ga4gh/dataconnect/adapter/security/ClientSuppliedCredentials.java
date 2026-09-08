package com.dnastack.ga4gh.dataconnect.adapter.security;

import com.dnastack.ga4gh.dataconnect.adapter.trino.exception.MalformedClientSuppliedCredentialsException;
import com.dnastack.tenancy.context.TenantContextAccessor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    /** The header these credentials arrive in, named in what a refusal tells the caller. */
    private static final String CREDENTIALS_HEADER = "GA4GH-Search-Authorization";

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
     * @param clientSuppliedCredentials the header's values, each a {@code name=value} pair. A blank value names
     * nothing and is skipped, which is what a header ending in a separator leaves behind.
     * @return the credentials by name
     * @throws MalformedClientSuppliedCredentialsException if a value is not a {@code name=value} pair, or names a
     * credential another value already named
     * @throws com.dnastack.ga4gh.dataconnect.adapter.trino.exception.RelayedTokenTenantMismatchException if a
     * supplied {@code userToken} names a tenant other than the request's
     */
    public Map<String, String> parse(List<String> clientSuppliedCredentials) {
        Map<String, String> credentials = new LinkedHashMap<>();
        for (String credential : clientSuppliedCredentials) {
            if (credential.isBlank()) {
                continue;
            }
            String[] nameAndValue = credential.split("=", 2);
            if (nameAndValue.length < 2) {
                // The credential is not echoed back: with no separator to read, the whole of it may be a value.
                throw new MalformedClientSuppliedCredentialsException("A credential in the "
                    + CREDENTIALS_HEADER + " header is not of the form name=value");
            }
            if (credentials.put(nameAndValue[0], nameAndValue[1]) != null) {
                throw new MalformedClientSuppliedCredentialsException("The credential " + nameAndValue[0]
                    + " is supplied more than once in the " + CREDENTIALS_HEADER + " header");
            }
        }

        String userToken = credentials.get(USER_TOKEN_CREDENTIAL);
        if (userToken != null) {
            userTokenTenancyValidator.ifPresent(validator ->
                validator.validate(tenantContextAccessor.getTenantId(), userToken));
        }

        return credentials;
    }
}
