package com.dnastack.ga4gh.search.adapter.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Component
@Slf4j
public class ScopeAuthoritiesConverter extends JwtAuthenticationConverter {

    private static List<String> KNOWN_SCOPE_CLAIMS = List.of("scope", "scopes", "scp");

    @Override
    protected Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        final Collection<String> scopes = extractTokenScopes(jwt);

        return scopes.stream()
                     .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                     .collect(toList());
    }

    private Collection<String> extractTokenScopes(Jwt jwt) {
        final List<String> definedScopeClaims = KNOWN_SCOPE_CLAIMS.stream()
                                                                  .filter(claimName -> jwt.getClaims().get(claimName) != null)
                                                                  .collect(toList());
        /*
         * If there are multiple known scopes defined, something may be wrong. This may even be an exploit of some kind
         * if we ever had an auth server that allowed clients to request custom claims
         */
        if (definedScopeClaims.size() > 1) {
            log.warn("Found multiple claims with scopes in token (not granting any authorities): {}", definedScopeClaims);
            return List.of();
        } else if (definedScopeClaims.size() == 1) {
            final String definedScopeClaim = definedScopeClaims.get(0);
            final Object scopes = jwt.getClaims().get(definedScopeClaim);

            if (scopes instanceof Collection) {
                return (Collection<String>) scopes;
            } else if (scopes instanceof String) {
                return Arrays.asList(((String) scopes).split("\\s+"));
            } else {
                log.warn("Unknown type [{}] for scope claim [{}]", scopes.getClass(), definedScopeClaim);
                return List.of();
            }
        } else {
            return List.of();
        }
    }
}