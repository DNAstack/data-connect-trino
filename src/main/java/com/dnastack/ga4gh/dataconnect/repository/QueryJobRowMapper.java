package com.dnastack.ga4gh.dataconnect.repository;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class QueryJobRowMapper implements RowMapper<QueryJob> {

    @Override
    public QueryJob map(ResultSet rs, StatementContext ctx) throws SQLException {
        QueryJob queryJob = new QueryJob();
        queryJob.setId(rs.getString("id"));
        queryJob.setQuery(rs.getString("query"));
        if (rs.getString("schema") != null) {
            queryJob.setSchema(rs.getString("schema"));
        }

        if (rs.getTimestamp("started_at") != null) {
            queryJob.setStartedAt(rs.getTimestamp("started_at").toInstant());
        }

        if (rs.getTimestamp("finished_at") != null) {
            queryJob.setFinishedAt(rs.getTimestamp("finished_at").toInstant());
        }

        if (rs.getTimestamp("last_activity_at") != null) {
            queryJob.setLastActivityAt(rs.getTimestamp("last_activity_at").toInstant());
        }

        return queryJob;
    }
}
