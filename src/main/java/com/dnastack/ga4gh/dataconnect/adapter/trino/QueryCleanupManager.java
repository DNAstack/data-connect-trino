package com.dnastack.ga4gh.dataconnect.adapter.trino;

import com.dnastack.ga4gh.dataconnect.repository.QueryJob;
import com.dnastack.ga4gh.dataconnect.repository.QueryJobDao;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class QueryCleanupManager {

    private final Jdbi jdbi;
    private final TrinoClient client;

    @Value("${app.query-cleanup-timeout-in-seconds}")
    private int queryCleanupTimeoutInSeconds;

    public QueryCleanupManager(Jdbi jdbi, TrinoClient client) {
        this.jdbi = jdbi;
        this.client = client;
    }

    @Scheduled(cron = "${app.query-cleanup-interval}")
    public void cleanupOldQueries() {
        List<QueryJob> queryJobList = jdbi.withExtension(QueryJobDao.class, dao -> {
            Instant oldQueryTimestamp = Instant.now().minusSeconds(queryCleanupTimeoutInSeconds);
            return dao.getOldQueries(oldQueryTimestamp);
        });
        if (queryJobList.isEmpty()) {
            log.info("No old queries present, skipping cleanup.");
        } else {
            log.info("Terminating {} old queries", queryJobList.size());
            queryJobList.forEach(queryJob -> {
                final String queryJobId = queryJob.getId();
                log.info("Terminating query with ID: {}", queryJobId);
                client.killQuery(queryJob.getNextPageUrl());
                jdbi.useExtension(QueryJobDao.class, dao -> {
                    dao.setFinishedAt(Instant.now(), queryJobId);
                });
            });
        }
    }

}
