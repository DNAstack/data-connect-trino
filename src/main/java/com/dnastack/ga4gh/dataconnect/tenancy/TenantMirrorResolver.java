package com.dnastack.ga4gh.dataconnect.tenancy;

import com.dnastack.tenancy.context.TenantId;
import com.dnastack.tenancy.context.TenantResolver;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Component;

/** Answers "is this tenant servable right now?" from tenant_mirror; the management sentinel always is. */
@Component
public class TenantMirrorResolver implements TenantResolver {

    private final Jdbi jdbi;

    public TenantMirrorResolver(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    public boolean isAccessible(TenantId tenantId) {
        return tenantId.isManagement()
                || jdbi.withExtension(TenantMirrorDao.class, dao -> dao.isEnabled(tenantId));
    }
}
