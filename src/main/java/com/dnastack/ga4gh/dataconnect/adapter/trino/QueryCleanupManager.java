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
import java.util.Optional;

@Slf4j
@Service
public class QueryCleanupManager {

    private final Jdbi jdbi;

    @Value("${app.query-cleanup-timeout-in-seconds}")
    private int queryCleanupTimeoutInSeconds;

    public QueryCleanupManager(Jdbi jdbi) {this.jdbi = jdbi;}

//    @Scheduled(cron = "${app.query-cleanup-interval}")
    @Scheduled(cron = "0/2 * * ? * *")
    public void cleanupOldQueries() {
        Optional<List<QueryJob>> optionalQueryJobList = jdbi.withExtension(QueryJobDao.class, dao -> {
            Instant oldQueryTimestamp = Instant.now().minusSeconds(queryCleanupTimeoutInSeconds);
            var test = dao.getOldQueries(oldQueryTimestamp);
            return test;
        });
        if (optionalQueryJobList.isEmpty()) {
            log.info("No old queries present, skipping cleanup.");
        } else {
            List<QueryJob> queryJobList = optionalQueryJobList.get();
            log.info("Terminating {} old queries", queryJobList.size());
            queryJobList.forEach(queryJob -> log.info("Terminating query with ID: {}", queryJob.getId()));
        }
    }

}
