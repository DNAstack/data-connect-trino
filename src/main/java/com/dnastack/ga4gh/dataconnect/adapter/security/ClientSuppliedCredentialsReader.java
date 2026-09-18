package com.dnastack.ga4gh.dataconnect.adapter.security;

import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoHttpClient;
import com.dnastack.ga4gh.dataconnect.adapter.trino.exception.MalformedClientSuppliedCredentialsException;
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
 * here. It returns the pairs that read the same way to every parser downstream, and refuses the names this
 * service asserts on its own authority, so no credential a caller invents can displace one of those, whatever
 * spelling it arrives in.
 */
@Component
public class ClientSuppliedCredentialsReader {

    /** The header these credentials arrive in, named in what a refusal tells the caller. */
    private static final String CREDENTIALS_HEADER = "GA4GH-Search-Authorization";

    /**
     * The credentials this service sends on its own authority, and so will not relay from a caller.
     * <p>
     * This is not a security boundary: a tenant indicator is caller-controlled by contract, and the service that
     * evaluates policy is the one that holds it to the token it was sent. It is about coherence. Trino takes the
     * last value given for a name, so a request carrying two of these would run under one tenant while this
     * service audits it under another, decided by the order they happen to be written in.
     */
    private static final Set<String> CREDENTIALS_THIS_SERVICE_ASSERTS =
        Set.of(TrinoHttpClient.TENANT_ID_CREDENTIAL, TrinoHttpClient.TRACEPARENT_CREDENTIAL);

    /**
     * One unambiguous {@code name=value} pair: a name of word characters, and a value of visible characters
     * that carry no meaning to anything the pair passes through.
     * <p>
     * This pattern refuses an ambiguous pair rather than resolving it, because parsers this service does not own
     * read the pair again downstream: Trino splits it on every {@code =} and trims each side, Trino's header
     * authenticator matches the name by prefix without trimming, and Spring splits the header on commas on the
     * way in. A pair that reads the same under all of them needs no agreement about which of them is right, so
     * the pattern excludes {@code =}, whitespace and {@code ,}, along with {@code %}, which Trino would
     * URL-decode into something this service never saw.
     */
    private static final Pattern CREDENTIAL = Pattern.compile("(\\w+)=([\\p{Graph}&&[^%,=]]+)");

    /**
     * @param clientSuppliedCredentials the header's values, each a {@code name=value} pair. A blank value names
     * nothing and is skipped, which is what a header ending in a separator leaves behind.
     * @return the credentials by name
     * @throws MalformedClientSuppliedCredentialsException if a value is not an unambiguous {@code name=value}
     * pair, or names a credential another value already named
     */
    public Map<String, String> parse(List<String> clientSuppliedCredentials) {
        Map<String, String> credentials = new LinkedHashMap<>();
        for (String credential : clientSuppliedCredentials) {
            if (credential.isBlank()) {
                continue;
            }
            Matcher pair = CREDENTIAL.matcher(credential);
            if (!pair.matches()) {
                // This does not echo the credential back: whatever is wrong with it, the whole of it may be a value.
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

        return credentials;
    }
}
