package com.dnastack.ga4gh.dataconnect.adapter.security;

import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoHttpClient;
import com.dnastack.ga4gh.dataconnect.adapter.trino.exception.MalformedClientSuppliedCredentialsException;
import com.dnastack.tenancy.context.TenantContextAccessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the extra credentials a caller sends in the {@code GA4GH-Search-Authorization} header, and decides
 * which of them this service relays to Trino.
 * <p>
 * This is the request boundary for those credentials: every handler that accepts the header reads it through
 * here. What leaves here is a set of pairs that read the same to every parser downstream, holding only the
 * names this service means to relay — so no credential a caller invents can displace one this service asserts
 * on its own authority, whatever spelling it arrives in.
 */
@Slf4j
@Component
public class ClientSuppliedCredentialsReader {

    /** The header these credentials arrive in, named in what a refusal tells the caller. */
    private static final String CREDENTIALS_HEADER = "GA4GH-Search-Authorization";

    private static final String USER_TOKEN_CREDENTIAL = "userToken";

    /**
     * The credentials this service sends on its own authority, and so will not relay from a caller.
     * <p>
     * Not a security boundary: a tenant indicator is caller-controlled by contract, and the service that
     * evaluates policy is the one that holds it to the token it was sent. It is coherence. Trino takes the
     * last value given for a name, so a request carrying two of these would run under one tenant while this
     * service audits it under another, and which one depends on the order they happen to be written in.
     */
    private static final Set<String> CREDENTIALS_THIS_SERVICE_ASSERTS =
        Set.of(TrinoHttpClient.TENANT_ID_CREDENTIAL, TrinoHttpClient.TRACEPARENT_CREDENTIAL);

    /**
     * One unambiguous {@code name=value} pair: a name of word characters, and a value of visible characters
     * that carry no meaning to anything the pair passes through.
     * <p>
     * Ambiguity is refused rather than resolved, because the pair is read again downstream by parsers this
     * service does not own and cannot follow: Trino splits the pair on every {@code =} and trims each side, its
     * header authenticator matches the name by prefix without trimming, and the header itself is split on
     * commas on the way in. A pair that reads the same under all of them needs no agreement about which of
     * them is right — so {@code =}, whitespace and {@code ,} are excluded here, along with {@code %}, which
     * Trino would URL-decode into something this service never saw.
     */
    private static final Pattern CREDENTIAL = Pattern.compile("(\\w+)=([\\p{Graph}&&[^%,=]]+)");

    private final TenantContextAccessor tenantContextAccessor;
    private final UserTokenTenancyValidator userTokenTenancyValidator;

    public ClientSuppliedCredentialsReader(
        TenantContextAccessor tenantContextAccessor,
        UserTokenTenancyValidator userTokenTenancyValidator
    ) {
        this.tenantContextAccessor = tenantContextAccessor;
        this.userTokenTenancyValidator = userTokenTenancyValidator;
    }

    /**
     * @param clientSuppliedCredentials the header's values, each a {@code name=value} pair. A blank value names
     * nothing and is skipped, which is what a header ending in a separator leaves behind.
     * @return the credentials by name
     * @throws MalformedClientSuppliedCredentialsException if a value is not an unambiguous {@code name=value}
     * pair, or names a credential another value already named
     * @throws com.dnastack.ga4gh.dataconnect.adapter.trino.exception.RelayedTokenTenantMismatchException if a
     * supplied {@code userToken} names a tenant other than the request's
     */
    public Map<String, String> parse(List<String> clientSuppliedCredentials) {
        Map<String, String> credentials = new LinkedHashMap<>();
        for (String credential : clientSuppliedCredentials) {
            if (credential.isBlank()) {
                continue;
            }
            Matcher pair = CREDENTIAL.matcher(credential);
            if (!pair.matches()) {
                // The credential is not echoed back: whatever is wrong with it, the whole of it may be a value.
                throw new MalformedClientSuppliedCredentialsException("A credential in the " + CREDENTIALS_HEADER
                    + " header is not of the form name=value, with a name of word characters and a value free of"
                    + " whitespace, '=', ',' and '%'");
            }
            String name = pair.group(1);
            if (CREDENTIALS_THIS_SERVICE_ASSERTS.contains(name)) {
                throw new MalformedClientSuppliedCredentialsException("The credential " + name + " in the "
                    + CREDENTIALS_HEADER + " header is sent by this service, and cannot be supplied with the"
                    + " request");
            }
            if (credentials.put(name, pair.group(2)) != null) {
                throw new MalformedClientSuppliedCredentialsException("The credential " + name
                    + " is supplied more than once in the " + CREDENTIALS_HEADER + " header");
            }
        }

        // Temporary (CU-86bbtvppq). A relayed token is the caller's own, addressed to the service that
        // evaluates data policy, and that service is the one that can hold it to a tenant -- it verifies the
        // token it was sent and knows the tenant the request asks about. Checking it here repeats that work
        // against an audience this service was never the audience of. It stands only until Trino passes the
        // request's tenant to collection-service and collection-service enforces it, and comes out then.
        String userToken = credentials.get(USER_TOKEN_CREDENTIAL);
        if (userToken != null) {
            userTokenTenancyValidator.validate(tenantContextAccessor.getTenantId(), userToken);
        }

        return credentials;
    }
}
