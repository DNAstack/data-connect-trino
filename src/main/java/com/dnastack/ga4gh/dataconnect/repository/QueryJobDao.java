package com.dnastack.ga4gh.dataconnect.repository;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlCall;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@RegisterBeanMapper(QueryJob.class)
public interface QueryJobDao {
    @SqlQuery("SELECT * FROM query_job WHERE id = :id")
    Optional<QueryJob> get(@Bind String id);

    @SqlUpdate("INSERT INTO query_job (id, query, schema, started_at, last_activity_at, next_page_url) VALUES (:id, :query, :schema, :startedAt, :lastActivityAt, :nextPageUrl)")
    void create(@BindBean QueryJob queryJob);

    @SqlUpdate("UPDATE query_job SET finished_at = :finishedAt WHERE id = :id")
    void setFinishedAt(@Bind Instant finishedAt, @Bind String id);

    @SqlUpdate("UPDATE query_job SET last_activity_at = :lastActivityAt where id = :id")
    void setLastActivityAt(@Bind Instant lastActivityAt, @Bind String id);

    @SqlUpdate("UPDATE query_job SET last_activity_at = now(), finished_at = now() where id = :id")
    void setQueryFinishedAndLastActivityTime(@Bind String id);

    @SqlQuery("SELECT * FROM query_job WHERE last_activity_at < :lastActivity AND finished_at IS NULL")
    @SqlQuery("SELECT * FROM query_job WHERE (last_activity_at IS NOT NULL AND started_at IS NOT NULL) AND last_activity_at < :lastActivity AND finished_at IS NULL")
    List<QueryJob> getOldQueries(@Bind Instant lastActivity);

    @SqlQuery("SELECT next_page_url FROM query_job WHERE id = :id")
    String getNextPageUrl(@Bind String id);
}
