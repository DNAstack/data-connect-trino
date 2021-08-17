package com.dnastack.ga4gh.dataconnect.repository;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Optional;

@RegisterBeanMapper(QueryJob.class)
public interface QueryJobDao {
    @SqlQuery("SELECT * FROM query_job WHERE id = :id")
    Optional<QueryJob> get(@Bind String id);

    @SqlUpdate("INSERT INTO query_job (id, query, schema) VALUES (:id, :query, :schema)")
    void save(@BindBean QueryJob queryJob);
}
