package com.dnastack.ga4gh.dataconnect.adapter.trino;

import com.dnastack.ga4gh.dataconnect.repository.QueryJob;
import com.dnastack.ga4gh.dataconnect.repository.QueryJobDao;
import com.dnastack.tenancy.context.TenantContextAccessor;
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
    private final TenantContextAccessor tenantContextAccessor;

    @Value("${app.query-cleanup.timeout-in-seconds}")
    private int queryCleanupTimeoutInSeconds;

    @Value("${app.query-job-cleanup.deletion-timeout-in-days}")
    private int queryJobCleanupDeletionTimeoutInDays;

    public QueryCleanupManager(Jdbi jdbi, TrinoClient client, TenantContextAccessor tenantContextAccessor) {
        this.jdbi = jdbi;
        this.client = client;
        this.tenantContextAccessor = tenantContextAccessor;
    }

    @Scheduled(cron = "${app.query-cleanup.cron-interval}")
    public void terminateOldQueries() {
        List<QueryJob> queryJobList = jdbi.withExtension(QueryJobDao.class, dao -> {
            Instant oldQueryTimestamp = Instant.now().minusSeconds(queryCleanupTimeoutInSeconds);
            return dao.getOldQueries(oldQueryTimestamp);
        });
        if (!queryJobList.isEmpty()) {
            log.info("Terminating {} old queries", queryJobList.size());
            // The sweep spans every tenant, but each row is acted on within its own: the update filters on the
            // tenant, and killQuery relays the tenant of whoever the work is being done for.
            queryJobList.forEach(queryJob -> tenantContextAccessor.runAs(queryJob.getTenantId(), () -> {
                final String queryJobId = queryJob.getId();
                log.info("Terminating query with ID: {}", queryJobId);
                client.killQuery(queryJob.getNextPageUrl());
                jdbi.useExtension(QueryJobDao.class,
                    dao -> dao.setFinishedAt(tenantContextAccessor.getTenantId(), Instant.now(), queryJobId));
            }));
        }
    }

    @Scheduled(cron = "${app.query-job-cleanup.cron-interval}")
    public void deleteOldQueryJobRows() {
        jdbi.useExtension(QueryJobDao.class, dao -> {
            var recordsDeleted = dao.deleteOldQueryJobs(queryJobCleanupDeletionTimeoutInDays);
            log.info("Deleted {} rows from the query_job table", recordsDeleted);
        });
    }

}
