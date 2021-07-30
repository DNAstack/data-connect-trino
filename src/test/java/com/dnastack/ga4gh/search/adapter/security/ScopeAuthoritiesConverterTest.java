package com.dnastack.ga4gh.search.adapter.security;

import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ScopeAuthoritiesConverterTest {

    @Test
    public void allUndefined() {
        final ScopeAuthoritiesConverter converter = new ScopeAuthoritiesConverter();
        final Jwt jwt = createJwt(Map.of());
        final Collection<GrantedAuthority> authorities = converter.extractAuthorities(jwt);
        assertEquals(List.of(), authorities);
    }

    @Test
    public void scopeStringDefined() {
        final ScopeAuthoritiesConverter converter = new ScopeAuthoritiesConverter();
        final Jwt jwt = createJwt(Map.of("scope", "a b"));
        final Collection<GrantedAuthority> authorities = converter.extractAuthorities(jwt);
        assertEquals(List.of(new SimpleGrantedAuthority("SCOPE_a"), new SimpleGrantedAuthority("SCOPE_b")), authorities);
    }

    @Test
    public void scopesListDefined() {
        final ScopeAuthoritiesConverter converter = new ScopeAuthoritiesConverter();
        final Jwt jwt = createJwt(Map.of("scopes", List.of("a", "b")));
        final Collection<GrantedAuthority> authorities = converter.extractAuthorities(jwt);
        assertEquals(List.of(new SimpleGrantedAuthority("SCOPE_a"), new SimpleGrantedAuthority("SCOPE_b")), authorities);
    }

    @Test
    public void scpListDefined() {
        final ScopeAuthoritiesConverter converter = new ScopeAuthoritiesConverter();
        final Jwt jwt = createJwt(Map.of("scp", List.of("a", "b")));
        final Collection<GrantedAuthority> authorities = converter.extractAuthorities(jwt);
        assertEquals(List.of(new SimpleGrantedAuthority("SCOPE_a"), new SimpleGrantedAuthority("SCOPE_b")), authorities);
    }

    @Test
    public void multipleDefined() {
        final ScopeAuthoritiesConverter converter = new ScopeAuthoritiesConverter();
        final Jwt jwt = createJwt(Map.of(
                "scope", "a b d",
                "scopes", List.of("a", "b", "c")
        ));
        final Collection<GrantedAuthority> authorities = converter.extractAuthorities(jwt);
        assertEquals(List.of(), authorities);
    }

    private Jwt createJwt(Map<String, Object> claims) {
        final Map<String, Object> allClaims = new HashMap<>(claims);
        allClaims.putAll(Map.of(
                "iss", "https://fake.issuer.dnastack.com",
                "aud", "https://fake.audience.dnastack.com"
        ));
        return new Jwt("faketokenvalue",
                       Instant.now().minusSeconds(10),
                       Instant.now().plusSeconds(600),
                       Map.of(
                               "alg", "RS256",
                               "kid", "abc123"
                       ),
                       allClaims);
    }
}