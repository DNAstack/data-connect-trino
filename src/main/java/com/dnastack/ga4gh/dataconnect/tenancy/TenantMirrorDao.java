package com.dnastack.ga4gh.dataconnect.tenancy;

import com.dnastack.tenancy.context.InstanceScoped;
import com.dnastack.tenancy.context.TenantId;
import com.dnastack.tenancy.lifecycle.model.TenantStatus;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.UUID;

/** The local mirror of wallet's tenant directory; written only from the tenant-lifecycle feed. */
@InstanceScoped(reason = "the tenant directory itself is instance-scoped data")
public interface TenantMirrorDao {

    /** Whether the mirror holds this tenant as servable. A tenant it has never seen is not. */
    default boolean isEnabled(TenantId id) {
        return hasStatus(id, TenantStatus.ENABLED);
    }

    @SqlQuery("SELECT EXISTS(SELECT 1 FROM tenant_mirror WHERE id = :id AND status = :status)")
    boolean hasStatus(@Bind("id") TenantId id, @Bind("status") TenantStatus status);

    @SqlUpdate("INSERT INTO tenant_mirror (id, name, status, updated_at) VALUES (:id, :name, :status, now())"
            + " ON CONFLICT (id) DO UPDATE SET name = :name, status = :status, updated_at = now()")
    void upsert(@Bind("id") UUID id, @Bind("name") String name, @Bind("status") TenantStatus status);

    @SqlUpdate("DELETE FROM tenant_mirror WHERE id = :id")
    void delete(@Bind("id") UUID id);
}
