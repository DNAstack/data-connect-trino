package com.dnastack.ga4gh.search.adapter.security;

import com.dnastack.auth.PermissionChecker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@ConditionalOnExpression("'${app.auth.authorization-type}' == 'bearer'")
public class AccessEvaluator {
    @Value("${app.url}")
    private String appUrl;

    @Autowired(required = false)
    private AccessEvaluatorMethod accessEvaluatorMethod;

    /**
     * Usage of this method:
     * @PreAuthorize("@accessEvaluator.canAccessResource('/api/endpoint', 'app:feature:read', 'openid')")
     * Add the above line with appropriate api endpoint, actions and scopes on a controller method
     * to preauthorize the request
     * Additionally, you can handle exceptions using @ExceptionHandler
     * @param requiredResource path to the api endpoint
     * @param requiredActions check actions defined in policy
     * @param requiredScopes check scopes defined in policy
     * @return boolean value specifying whether the user can access the resource
     */
    public boolean canAccessResource(String requiredResource, Set<String> requiredActions, Set<String> requiredScopes) {
        if (this.accessEvaluatorMethod == null) {
            useDefaultAccessEvaluatorMethod();
        }
        return this.accessEvaluatorMethod.checkAccessResource(requiredResource, requiredActions, requiredScopes);
    }

    public static abstract class AccessEvaluatorMethod {
        public abstract boolean checkAccessResource(String requiredResource, Set<String> requiredActions, Set<String> requiredScopes);
    }

    @ConditionalOnClass(name = "com.dnastack.auth.PermissionChecker")
    private static class WalletAccessEvaluatorMethod extends AccessEvaluatorMethod {
        private final String appUrl;
        private final PermissionChecker permissionChecker;

        WalletAccessEvaluatorMethod(String appUrl, PermissionChecker permissionChecker) {
            this.appUrl = appUrl;
            this.permissionChecker = permissionChecker;
        }

        @Override
        public boolean checkAccessResource(String requiredResource, Set<String> requiredActions, Set<String> requiredScopes) {
            final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                log.warn("Authentication must be present in context, resolving access as denied.");
                return false;
            }
            if (!(authentication.getPrincipal() instanceof Jwt)) {
                log.warn("Principal must be type of {}, resolving access as denied.", Jwt.class.getName());
                return false;
            }
            return Optional.ofNullable(authentication.getPrincipal())
                    .map((principal) -> (Jwt) principal)
                    .map((jwtPrincipal) -> {
                        final String fullResourceUrl = appUrl + requiredResource;
                        boolean hasPermissions = permissionChecker.hasPermissions(jwtPrincipal.getTokenValue(), requiredScopes, fullResourceUrl, requiredActions);
                        if (!hasPermissions) {
                            log.info("Denying access to {} for {}. requiredScopes={}; requiredActions={}; actualScopes={}; actualActions={}",
                                    jwtPrincipal.getSubject(), fullResourceUrl, requiredScopes, requiredActions,
                                    jwtPrincipal.getClaims().get("scope"), jwtPrincipal.getClaims().get("actions"));
                        }
                        return hasPermissions;
                    })
                    .orElse(false);
        }
    }

    @Bean
    @ConditionalOnClass(name = "com.dnastack.auth.PermissionChecker")
    @ConditionalOnExpression("'${app.auth.access-evaluator}' == 'wallet'")
    public AccessEvaluatorMethod walletAccessEvaluatorMethod(PermissionChecker permissionChecker) {
        return new WalletAccessEvaluatorMethod(appUrl, permissionChecker);
    }

    private void useDefaultAccessEvaluatorMethod() {
        this.accessEvaluatorMethod = new AccessEvaluatorMethod() {
            @Override
            public boolean checkAccessResource(String requiredResource, Set<String> requiredActions, Set<String> requiredScopes) {
                return true;
            }
        };
    }

}
