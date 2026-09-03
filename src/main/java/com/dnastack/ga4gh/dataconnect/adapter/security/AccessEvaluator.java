package com.dnastack.ga4gh.dataconnect.adapter.security;

import com.dnastack.auth.PermissionChecker;
import com.dnastack.tenancy.context.TenantContextAccessor;
import com.dnastack.tenancy.context.TenantId;
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

    @Autowired
    private AccessEvaluatorMethod accessEvaluatorMethod;

    @Autowired
    private TenantContextAccessor tenantContextAccessor;

    /**
     * Usage of this method:
     * @PreAuthorize("@accessEvaluator.canAccessTenantResource('/api/endpoint', 'app:feature:read', 'openid')")
     * Add the above line with appropriate api endpoint, actions and scopes on a controller method
     * to preauthorize the request
     * Additionally, you can handle exceptions using @ExceptionHandler
     * <p>
     * Guards a tenant-scoped resource: the token's tenant claim must match the request's tenant (resolved from
     * the {tenantId} path variable; legacy paths resolve to the management tenant), a management-tenant token
     * passing for any tenant. The resource URI itself carries no tenant, so one policy covers every tenant.
     * @param requiredResource path to the api endpoint
     * @param requiredActions check actions defined in policy
     * @param requiredScopes check scopes defined in policy
     * @return boolean value specifying whether the user can access the resource
     */
    public boolean canAccessTenantResource(String requiredResource, Set<String> requiredActions, Set<String> requiredScopes) {
        return accessEvaluatorMethod.checkAccessResource(tenantContextAccessor.getTenantId(), requiredResource, requiredActions, requiredScopes);
    }

    public static abstract class AccessEvaluatorMethod {
        public abstract boolean checkAccessResource(TenantId expectedTenantId, String requiredResource, Set<String> requiredActions, Set<String> requiredScopes);
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
        public boolean checkAccessResource(TenantId expectedTenantId, String requiredResource, Set<String> requiredActions, Set<String> requiredScopes) {
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
                        boolean hasPermissions = permissionChecker.hasPermissions(jwtPrincipal.getTokenValue(),
                                expectedTenantId.getValue(), requiredScopes, fullResourceUrl, requiredActions);
                        if (!hasPermissions) {
                            log.info("Denying access to {} for {}. tenant={}; requiredScopes={}; requiredActions={}; actualScopes={}; actualActions={}",
                                    jwtPrincipal.getSubject(), fullResourceUrl, expectedTenantId.asString(), requiredScopes, requiredActions,
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
    public AccessEvaluatorMethod walletAccessEvaluator(PermissionChecker permissionChecker) {
        return new WalletAccessEvaluatorMethod(appUrl, permissionChecker);
    }

    @Bean
    @ConditionalOnExpression("'${app.auth.authorization-type}' == 'none'")
    private AccessEvaluatorMethod allowAllAccessEvaluator() {
        return new AccessEvaluatorMethod() {
            @Override
            public boolean checkAccessResource(TenantId expectedTenantId, String requiredResource, Set<String> requiredActions, Set<String> requiredScopes) {
                return true;
            }
        };
    }

}
