package com.dnastack.ga4gh.dataconnect.tenancy;

import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoClient;
import com.dnastack.ga4gh.dataconnect.repository.QueryJob;
import com.dnastack.ga4gh.dataconnect.repository.QueryJobDao;
import com.dnastack.tenancy.context.TenantContextAccessor;
import com.dnastack.tenancy.context.TenantId;
import com.dnastack.tenancy.lifecycle.TenantLifecycleHandler;
import com.dnastack.tenancy.lifecycle.model.TenantSnapshot;
import com.dnastack.tenancy.lifecycle.model.TenantStatus;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Keeps tenant_mirror in step with wallet's tenant change feed, and tears down a tenant's query jobs when wallet
 * hard-deletes it. All callbacks are idempotent, as the feed contract requires.
 */
@Slf4j
@Component
public class TenantMirrorLifecycleHandler implements TenantLifecycleHandler {

    private final Jdbi jdbi;
    private final TrinoClient client;
    private final TenantContextAccessor tenantContextAccessor;

    public TenantMirrorLifecycleHandler(Jdbi jdbi, TrinoClient client, TenantContextAccessor tenantContextAccessor) {
        this.jdbi = jdbi;
        this.client = client;
        this.tenantContextAccessor = tenantContextAccessor;
    }

    @Override
    public void onTenantEnabled(TenantSnapshot tenant) {
        jdbi.useExtension(TenantMirrorDao.class,
                dao -> dao.upsert(tenant.getId(), tenant.getName(), TenantStatus.ENABLED.name()));
        log.info("Tenant [{}] mirrored as ENABLED", tenant.getId());
    }

    @Override
    public void onTenantDisabled(TenantSnapshot tenant) {
        jdbi.useExtension(TenantMirrorDao.class,
                dao -> dao.upsert(tenant.getId(), tenant.getName(), TenantStatus.DISABLED.name()));
        log.info("Tenant [{}] mirrored as DISABLED", tenant.getId());
    }

    @Override
    public void onTenantDeleted(UUID tenantId) {
        tenantContextAccessor.runAs(tenantId, () -> {
            TenantId tenant = tenantContextAccessor.getTenantId();

            // Deleting the rows takes away the pages the cleanup sweep would have used to terminate these
            // queries, so they are terminated here while the pages are still on hand.
            List<QueryJob> running = jdbi.withExtension(QueryJobDao.class, dao -> dao.getUnfinished(tenant));
            running.forEach(queryJob -> {
                log.info("Terminating query {} of deleted tenant {}", queryJob.getId(), tenantId);
                client.killQuery(queryJob.getNextPageUrl());
            });

            jdbi.useTransaction(handle -> {
                handle.attach(QueryJobDao.class).deleteAll(tenant);
                handle.attach(TenantMirrorDao.class).delete(tenantId);
            });
        });
        log.info("Tenant [{}] deleted; its query jobs were purged", tenantId);
    }
}
