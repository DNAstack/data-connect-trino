package com.dnastack.ga4gh.dataconnect.tenancy;

import com.dnastack.tenancy.context.InstanceScoped;
import com.dnastack.tenancy.context.TenantId;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import java.util.UUID;

/** The local mirror of wallet's tenant directory; written only from the tenant-lifecycle feed. */
@InstanceScoped(reason = "the tenant directory itself is instance-scoped data")
public interface TenantMirrorDao {

    @SqlQuery("SELECT EXISTS(SELECT 1 FROM tenant_mirror WHERE id = :id AND status = 'ENABLED')")
    boolean isEnabled(@Bind("id") TenantId id);

    @Transaction
    @SqlUpdate("INSERT INTO tenant_mirror (id, name, status, updated_at) VALUES (:id, :name, :status, now())"
            + " ON CONFLICT (id) DO UPDATE SET name = :name, status = :status, updated_at = now()")
    void upsert(@Bind("id") UUID id, @Bind("name") String name, @Bind("status") String status);

    @SqlUpdate("DELETE FROM tenant_mirror WHERE id = :id")
    void delete(@Bind("id") UUID id);
}
