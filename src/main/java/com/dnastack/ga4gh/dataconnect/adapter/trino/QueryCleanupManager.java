package com.dnastack.ga4gh.dataconnect.adapter.trino;

import com.dnastack.ga4gh.dataconnect.repository.QueryJob;
import com.dnastack.ga4gh.dataconnect.repository.QueryJobDao;
import com.dnastack.tenancy.context.TenantContextAccessor;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.List;

@Slf4j
@Service
public class QueryCleanupManager {

    private final Jdbi jdbi;
    private final TrinoClient client;
    private final TenantContextAccessor tenantContextAccessor;

    @Value("${app.query-cleanup.timeout-in-seconds}")
    private int queryCleanupTimeoutInSeconds;

    /**
     * How long a query may go on resisting cancellation before the sweep stops asking about it. This counts from
     * the same last activity as {@link #queryCleanupTimeoutInSeconds} and must be configured larger than it:
     * a query becomes eligible for cancellation at queryCleanupTimeoutInSeconds, and is written off after this time.
     */
    @Value("${app.query-cleanup.give-up-after-seconds}")
    private int giveUpAfterSeconds;

    @Value("${app.query-job-cleanup.deletion-timeout-in-days}")
    private int queryJobCleanupDeletionTimeoutInDays;

    public QueryCleanupManager(Jdbi jdbi, TrinoClient client, TenantContextAccessor tenantContextAccessor) {
        this.jdbi = jdbi;
        this.client = client;
        this.tenantContextAccessor = tenantContextAccessor;
    }

    /**
     * Fails startup rather than letting the sweep run with the two thresholds the wrong way round, which would
     * write a query off on the same sweep that first tries to cancel it.
     */
    @PostConstruct
    void checkCleanupTimeouts() {
        requireGiveUpAfterExceedsTimeout(giveUpAfterSeconds, queryCleanupTimeoutInSeconds);
    }

    /**
     * @throws IllegalStateException if {@code giveUpAfterSeconds} does not exceed {@code timeoutInSeconds}. Both
     * count from a query's last activity: a query becomes eligible for cancellation at the one and is written off
     * at the other, so an order the wrong way round leaves no sweep in between to do the cancelling.
     */
    static void requireGiveUpAfterExceedsTimeout(int giveUpAfterSeconds, int timeoutInSeconds) {
        if (giveUpAfterSeconds <= timeoutInSeconds) {
            throw new IllegalStateException("app.query-cleanup.give-up-after-seconds (" + giveUpAfterSeconds
                + ") must exceed app.query-cleanup.timeout-in-seconds (" + timeoutInSeconds + ")");
        }
    }

    @Scheduled(cron = "${app.query-cleanup.cron-interval}")
    public void terminateOldQueries() {
        List<QueryJob> queryJobList = jdbi.withExtension(QueryJobDao.class, dao -> {
            Instant oldQueryTimestamp = Instant.now().minusSeconds(queryCleanupTimeoutInSeconds);
            return dao.getOldQueries(oldQueryTimestamp);
        });
        if (!queryJobList.isEmpty()) {
            log.info("Terminating {} old queries", queryJobList.size());
            // The sweep spans every tenant, and runs each row in the tenant that row belongs to: the update
            // filters on that tenant, and the cancellation relays it to Trino.
            queryJobList.forEach(queryJob -> tenantContextAccessor.runAs(queryJob.getTenantId(), () -> {
                final String queryJobId = queryJob.getId();
                log.info("Terminating query with ID: {}", queryJobId);
                if (cancelQuery(queryJob)) {
                    markFinished(queryJobId);
                } else if (idleSinceBeforeGivingUp(queryJob)) {
                    // Retrying this one forever would cost a Trino call every sweep for as long as the row
                    // lives. Trino ages its own queries out, so the worst this concedes is a query that
                    // outlives our record of it.
                    log.warn("Query {} has resisted cancellation since {}; recording it as finished and "
                        + "leaving it to Trino's own timeout", queryJobId, queryJob.getLastActivityAt());
                    markFinished(queryJobId);
                }
                // Otherwise the job stays unfinished on purpose, for the next sweep to try again.
            }));
        }
    }

    /**
     * Asks Trino to cancel the query associated with the given job.
     * <p>
     * A 404 counts as cancelled. The page relayed here is the one Trino itself handed back and this service
     * stored, so Trino not recognising it says the query has ended rather than that the page was never issued —
     * the opposite of what the same status means for the page a caller supplies to {@code DELETE /search/**}.
     *
     * @return whether the query is known to have stopped, either because this call ended it or because it was
     * already over. False where Trino refused or could not be reached, which leaves the job for a later sweep.
     */
    private boolean cancelQuery(QueryJob queryJob) {
        try {
            int status = client.cancelQuery(queryJob.getNextPageUrl(), Map.of());
            // This compares the status as a range rather than through HttpStatus, which rejects a status it does
            // not know: an intermediary is free to answer 520, and that is a refusal to retry, not a bad status.
            if ((status >= 200 && status <= 299) || status == HttpStatus.NOT_FOUND.value()) {
                log.info("Trino answered {} to the cancellation of query {}", status, queryJob.getId());
                return true;
            }
            log.warn("Trino answered {} to the cancellation of query {}", status, queryJob.getId());
            return false;
        } catch (RuntimeException e) {
            // Catching this keeps one query Trino will not answer from ending the sweep over every other row.
            log.warn("Could not reach Trino to cancel query {}", queryJob.getId(), e);
            return false;
        }
    }

    /** Whether this query has been idle long enough that the sweep stops asking Trino about it. */
    private boolean idleSinceBeforeGivingUp(QueryJob queryJob) {
        return queryJob.getLastActivityAt().isBefore(Instant.now().minusSeconds(giveUpAfterSeconds));
    }

    private void markFinished(String queryJobId) {
        jdbi.useExtension(QueryJobDao.class,
            dao -> dao.setFinishedAt(tenantContextAccessor.getTenantId(), Instant.now(), queryJobId));
    }

    @Scheduled(cron = "${app.query-job-cleanup.cron-interval}")
    public void deleteOldQueryJobRows() {
        jdbi.useExtension(QueryJobDao.class, dao -> {
            var recordsDeleted = dao.deleteOldQueryJobs(queryJobCleanupDeletionTimeoutInDays);
            log.info("Deleted {} rows from the query_job table", recordsDeleted);
        });
    }

}
