package com.dnastack.ga4gh.dataconnect.repository;

import com.dnastack.tenancy.context.InstanceScoped;
import com.dnastack.tenancy.context.TenantId;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@RegisterBeanMapper(QueryJob.class)
public interface QueryJobDao {

    @SqlQuery("SELECT * FROM query_job WHERE id = :id AND tenant_id = :tenantId")
    Optional<QueryJob> get(@Bind TenantId tenantId, @Bind String id);

    @SqlUpdate("INSERT INTO query_job (id, tenant_id, original_trace_id, query, schema, started_at, last_activity_at, next_page_url) VALUES (:id, :tenantId, :originalTraceId, :query, :schema, :startedAt, :lastActivityAt, :nextPageUrl)")
    void create(@BindBean QueryJob queryJob);

    @SqlUpdate("UPDATE query_job SET finished_at = :finishedAt WHERE id = :id AND tenant_id = :tenantId")
    void setFinishedAt(@Bind TenantId tenantId, @Bind Instant finishedAt, @Bind String id);

    @SqlUpdate("UPDATE query_job SET last_activity_at = :lastActivityAt WHERE id = :id AND tenant_id = :tenantId")
    void setLastActivityAt(@Bind TenantId tenantId, @Bind Instant lastActivityAt, @Bind String id);

    @SqlUpdate("UPDATE query_job SET last_activity_at = NOW(), finished_at = NOW() WHERE id = :id AND tenant_id = :tenantId")
    void setQueryFinishedAndLastActivityTime(@Bind TenantId tenantId, @Bind String id);

    @SqlQuery("SELECT * FROM query_job WHERE next_page_url IS NOT NULL AND finished_at IS NULL AND tenant_id = :tenantId")
    List<QueryJob> getUnfinished(@Bind TenantId tenantId);

    @SqlUpdate("DELETE FROM query_job WHERE tenant_id = :tenantId")
    void deleteAll(@Bind TenantId tenantId);

    @InstanceScoped(reason = "the cleanup sweep terminates every tenant's abandoned queries, and acts on each row in its own tenant")
    @SqlQuery("SELECT * FROM query_job WHERE next_page_url IS NOT NULL AND last_activity_at < :lastActivity AND finished_at IS NULL")
    List<QueryJob> getOldQueries(@Bind Instant lastActivity);

    @InstanceScoped(reason = "the retention sweep is age-based and instance-wide by design")
    @SqlUpdate("DELETE FROM query_job WHERE id IN (SELECT id FROM query_job WHERE last_activity_at < NOW()::DATE - :timeoutInDays)")
    int deleteOldQueryJobs(@Bind int timeoutInDays);

}
