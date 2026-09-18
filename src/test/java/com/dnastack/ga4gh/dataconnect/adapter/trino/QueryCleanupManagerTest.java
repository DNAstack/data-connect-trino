package com.dnastack.ga4gh.dataconnect.adapter.trino;

import com.dnastack.ga4gh.dataconnect.DataConnectTrinoApplication;
import com.dnastack.ga4gh.dataconnect.adapter.trino.exception.TrinoIOException;
import com.dnastack.ga4gh.dataconnect.repository.QueryJob;
import com.dnastack.ga4gh.dataconnect.repository.QueryJobDao;
import com.dnastack.tenancy.context.TenantId;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.jdbi.v3.core.Jdbi;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The sweep that terminates abandoned queries. It runs unattended over every tenant's rows, so a query Trino
 * will not answer must cost only that query.
 */
@AutoConfigureEmbeddedDatabase(provider = ZONKY, refresh = AFTER_EACH_TEST_METHOD, type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
        // MOCK, not NONE: the tenancy context auto-configuration only contributes to a servlet application, and
        // the sweep runs each row inside a tenant context.
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = DataConnectTrinoApplication.class,
        properties = {"management.tracing.enabled=false", "tenant-lifecycle.enabled=false"}
)
@ActiveProfiles("no-auth")
public class QueryCleanupManagerTest {

    private static final String UNREACHABLE_PAGE = "http://trino.example.com/v1/statement/executing/unreachable/s/1";
    private static final String REACHABLE_PAGE = "http://trino.example.com/v1/statement/executing/reachable/s/1";

    @Autowired
    private QueryCleanupManager queryCleanupManager;

    @Autowired
    private Jdbi jdbi;

    @MockitoBean
    private TrinoClient trinoClient;

    /**
     * A query idle long enough for the sweep to pick it up, but not long enough for it to give up on: it is
     * still worth asking Trino about, so a refusal leaves it for the next sweep.
     */
    private void abandonedQueryJob(String queryJobId, String nextPageUrl) {
        abandonedQueryJob(queryJobId, nextPageUrl, Duration.ofMinutes(5));
    }

    /** A query whose last activity was {@code idleFor} ago. */
    private void abandonedQueryJob(String queryJobId, String nextPageUrl, Duration idleFor) {
        Instant lastActivity = Instant.now().minus(idleFor);
        jdbi.useExtension(QueryJobDao.class, dao -> dao.create(QueryJob.builder()
                .id(queryJobId)
                .tenantId(TenantId.MANAGEMENT.getValue())
                .query("SELECT 1")
                .startedAt(lastActivity)
                .lastActivityAt(lastActivity)
                .nextPageUrl(nextPageUrl)
                .build()));
    }

    private Optional<QueryJob> queryJob(String queryJobId) {
        return jdbi.withExtension(QueryJobDao.class, dao -> dao.get(TenantId.MANAGEMENT, queryJobId));
    }

    @Test
    public void terminateOldQueries_should_markEveryQueryFinished() {
        abandonedQueryJob("query-a", REACHABLE_PAGE);
        abandonedQueryJob("query-b", REACHABLE_PAGE);
        when(trinoClient.cancelQuery(eq(REACHABLE_PAGE), anyMap())).thenReturn(204);

        queryCleanupManager.terminateOldQueries();

        assertThat(queryJob("query-a")).get().extracting(QueryJob::getFinishedAt).as("query-a").isNotNull();
        assertThat(queryJob("query-b")).get().extracting(QueryJob::getFinishedAt).as("query-b").isNotNull();
    }

    @Test
    public void terminateOldQueries_should_sweepOnPast_when_trinoWillNotAnswerForOneQuery() {
        abandonedQueryJob("query-unreachable", UNREACHABLE_PAGE);
        abandonedQueryJob("query-reachable", REACHABLE_PAGE);
        when(trinoClient.cancelQuery(eq(UNREACHABLE_PAGE), anyMap()))
                .thenThrow(new TrinoIOException("Trino is unreachable", new IOException("connection refused")));
        when(trinoClient.cancelQuery(eq(REACHABLE_PAGE), anyMap())).thenReturn(204);

        queryCleanupManager.terminateOldQueries();

        assertThat(queryJob("query-reachable")).get()
                .extracting(QueryJob::getFinishedAt)
                .as("the query swept after one Trino would not answer for")
                .isNotNull();
    }

    @Test
    public void terminateOldQueries_shouldNot_markAQueryFinished_when_trinoWillNotAnswerForIt() {
        // The sweep leaves the row unfinished on purpose, so the next sweep tries it again rather than leaving
        // the query running in Trino.
        abandonedQueryJob("query-unreachable", UNREACHABLE_PAGE);
        when(trinoClient.cancelQuery(eq(UNREACHABLE_PAGE), anyMap()))
                .thenThrow(new TrinoIOException("Trino is unreachable", new IOException("connection refused")));

        queryCleanupManager.terminateOldQueries();

        assertThat(queryJob("query-unreachable")).get()
                .extracting(QueryJob::getFinishedAt)
                .as("the query Trino would not answer for")
                .isNull();
    }

    @Test
    public void terminateOldQueries_should_markAQueryFinished_when_trinoDoesNotRecognizeItsPage() {
        // The page relayed here is the one Trino handed back and this service stored, so Trino not knowing it
        // means the query has already ended. Asking again would 404 for as long as the row lives.
        abandonedQueryJob("query-already-over", REACHABLE_PAGE);
        when(trinoClient.cancelQuery(eq(REACHABLE_PAGE), anyMap())).thenReturn(404);

        queryCleanupManager.terminateOldQueries();

        assertThat(queryJob("query-already-over")).get()
                .extracting(QueryJob::getFinishedAt)
                .as("a query Trino no longer knows about")
                .isNotNull();
    }

    @Test
    public void terminateOldQueries_shouldNot_markAQueryFinished_when_trinoRefusesTheCancellation() {
        // Trino answers, but not with a cancellation: the query may well still be running, so the row stays
        // open for the next sweep rather than being recorded as something it is not.
        abandonedQueryJob("query-refused", UNREACHABLE_PAGE);
        when(trinoClient.cancelQuery(eq(UNREACHABLE_PAGE), anyMap())).thenReturn(503);

        queryCleanupManager.terminateOldQueries();

        assertThat(queryJob("query-refused")).get()
                .extracting(QueryJob::getFinishedAt)
                .as("a query whose cancellation Trino refused")
                .isNull();
    }

    @Test
    public void terminateOldQueries_should_markAQueryFinished_when_itHasResistedCancellationPastTheGiveUpTimeout() {
        // Without this, a query Trino will never cancel would be retried every sweep until the row is purged days
        // later. Trino ages its own queries out, so writing it off here concedes little.
        abandonedQueryJob("query-zombie", UNREACHABLE_PAGE, Duration.ofMinutes(20));
        when(trinoClient.cancelQuery(eq(UNREACHABLE_PAGE), anyMap()))
                .thenThrow(new TrinoIOException("Trino is unreachable", new IOException("connection refused")));

        queryCleanupManager.terminateOldQueries();

        assertThat(queryJob("query-zombie")).get()
                .extracting(QueryJob::getFinishedAt)
                .as("a query still resisting cancellation past the give-up timeout")
                .isNotNull();
    }

    @Test
    public void terminateOldQueries_should_keepAskingAboutAQueryUntilTheGiveUpTimeout() {
        abandonedQueryJob("query-recently-idle", UNREACHABLE_PAGE, Duration.ofMinutes(5));
        when(trinoClient.cancelQuery(eq(UNREACHABLE_PAGE), anyMap()))
                .thenThrow(new TrinoIOException("Trino is unreachable", new IOException("connection refused")));

        queryCleanupManager.terminateOldQueries();

        assertThat(queryJob("query-recently-idle")).get()
                .extracting(QueryJob::getFinishedAt)
                .as("a query that has not yet been idle long enough to write off")
                .isNull();
    }

    @Test
    public void terminateOldQueries_should_relayNoCallerCredentials() {
        abandonedQueryJob("query-a", REACHABLE_PAGE);
        when(trinoClient.cancelQuery(eq(REACHABLE_PAGE), anyMap())).thenReturn(204);

        queryCleanupManager.terminateOldQueries();

        verify(trinoClient).cancelQuery(REACHABLE_PAGE, Map.of());
    }

    @Test
    public void requireGiveUpAfterExceedsTimeout_should_returnNormally_when_givingUpComesAfterTheTimeout() {
        QueryCleanupManager.requireGiveUpAfterExceedsTimeout(900, 120);
    }

    @Test
    public void requireGiveUpAfterExceedsTimeout_should_throwIllegalState_when_theTwoThresholdsAreEqual() {
        // Equal leaves no sweep between becoming eligible for cancellation and being written off, so a query
        // would be recorded as finished on the same sweep that first asks Trino to cancel it.
        assertThatThrownBy(() -> QueryCleanupManager.requireGiveUpAfterExceedsTimeout(120, 120))
                .as("configuring the two cleanup thresholds the same")
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.query-cleanup.give-up-after-seconds")
                .hasMessageContaining("app.query-cleanup.timeout-in-seconds");
    }

    @Test
    public void requireGiveUpAfterExceedsTimeout_should_throwIllegalState_when_givingUpComesFirst() {
        assertThatThrownBy(() -> QueryCleanupManager.requireGiveUpAfterExceedsTimeout(60, 120))
                .as("configuring the sweep to give up before it would cancel")
                .isInstanceOf(IllegalStateException.class);
    }
}
