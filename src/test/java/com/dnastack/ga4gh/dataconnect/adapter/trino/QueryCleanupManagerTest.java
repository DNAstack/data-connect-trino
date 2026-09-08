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
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The sweep that terminates abandoned queries. It runs unattended over every tenant's rows, so a query Trino will
 * not answer for has to cost only that query.
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

    /** A query abandoned long enough ago that the sweep will pick it up. */
    private void abandonedQueryJob(String queryJobId, String nextPageUrl) {
        Instant longAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        jdbi.useExtension(QueryJobDao.class, dao -> dao.create(QueryJob.builder()
                .id(queryJobId)
                .tenantId(TenantId.MANAGEMENT.getValue())
                .query("SELECT 1")
                .startedAt(longAgo)
                .lastActivityAt(longAgo)
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
        // Left unfinished on purpose, so the next sweep tries it again rather than leaving it running in Trino.
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
    public void terminateOldQueries_should_relayNoCallerCredentials() {
        abandonedQueryJob("query-a", REACHABLE_PAGE);
        when(trinoClient.cancelQuery(eq(REACHABLE_PAGE), anyMap())).thenReturn(204);

        queryCleanupManager.terminateOldQueries();

        verify(trinoClient).cancelQuery(REACHABLE_PAGE, Map.of());
    }
}
