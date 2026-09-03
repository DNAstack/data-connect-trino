package com.dnastack.ga4gh.dataconnect;

import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoClient;
import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoDataPage;
import com.dnastack.ga4gh.dataconnect.repository.QueryJob;
import com.dnastack.ga4gh.dataconnect.repository.QueryJobDao;
import com.dnastack.ga4gh.dataconnect.tenancy.TenantMirrorLifecycleHandler;
import com.dnastack.tenancy.context.TenantContextAccessor;
import com.dnastack.tenancy.context.TenantId;
import com.dnastack.tenancy.context.TestTenantIds;
import com.dnastack.tenancy.lifecycle.model.TenantSnapshot;
import com.dnastack.tenancy.lifecycle.model.TenantStatus;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.jdbi.v3.core.Jdbi;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cross-tenant negative suite: a query job created in one tenant must not be readable, followable or cancellable
 * from another, an unknown or disabled tenant must be turned away at the request boundary before any of that,
 * and the legacy un-prefixed paths must keep behaving as they did — as the management tenant.
 */
@AutoConfigureEmbeddedDatabase(provider = ZONKY, refresh = AFTER_EACH_TEST_METHOD, type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = DataConnectTrinoApplication.class,
        properties = {"management.tracing.enabled=false", "tenant-lifecycle.enabled=false"}
)
@AutoConfigureMockMvc
@ActiveProfiles("no-auth")
public class TenantIsolationIntegrationTest {

    /** A page as Trino would issue it for {@link #QUERY_ID}, relayed back by a caller. */
    private static final String QUERY_ID = "20260903_120000_00001_abcde";
    private static final String PAGE = "v1/statement/executing/" + QUERY_ID + "/y5bb5cace5500a2cf109b1c50c648b009c40a142f/2";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Jdbi jdbi;

    @Autowired
    private TenantContextAccessor tenantContextAccessor;

    @Autowired
    private TenantMirrorLifecycleHandler lifecycleHandler;

    @MockBean
    private TrinoClient trinoClient;

    @Before
    public void stubTrino() {
        when(trinoClient.next(anyString(), anyMap()))
                .thenReturn(new TrinoDataPage(QUERY_ID, null, null, null, null, null, null));
        when(trinoClient.cancelQuery(anyString(), anyMap())).thenReturn(204);
    }

    private UUID enabledTenant() {
        UUID tenantId = UUID.randomUUID();
        lifecycleHandler.onTenantEnabled(TenantSnapshot.builder()
                .id(tenantId)
                .name("tenant-" + tenantId)
                .status(TenantStatus.ENABLED)
                .build());
        return tenantId;
    }

    private UUID disabledTenant() {
        UUID tenantId = enabledTenant();
        lifecycleHandler.onTenantDisabled(TenantSnapshot.builder()
                .id(tenantId)
                .name("tenant-" + tenantId)
                .status(TenantStatus.DISABLED)
                .build());
        return tenantId;
    }

    /** A running query job of the given tenant, as {@code POST /search} would have left behind. */
    private void queryJobOf(UUID tenantId) {
        Instant now = Instant.now();
        jdbi.useExtension(QueryJobDao.class, dao -> dao.create(QueryJob.builder()
                .id(QUERY_ID)
                .tenantId(tenantId)
                .query("SELECT 1")
                .startedAt(now)
                .lastActivityAt(now)
                .nextPageUrl("http://trino.example.com/" + PAGE)
                .build()));
    }

    private Optional<QueryJob> queryJobIn(UUID tenantId) {
        return jdbi.withExtension(QueryJobDao.class, dao -> dao.get(TestTenantIds.of(tenantId), QUERY_ID));
    }

    @Test
    public void getNextPage_should_returnNotFound_when_theQueryJobBelongsToAnotherTenant() throws Exception {
        queryJobOf(enabledTenant());
        UUID otherTenant = enabledTenant();

        mockMvc.perform(get("/tenants/" + otherTenant + "/search/" + PAGE).param("queryJobId", QUERY_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    public void deleteQueryJob_should_returnNotFound_when_theQueryJobBelongsToAnotherTenant() throws Exception {
        UUID owningTenant = enabledTenant();
        queryJobOf(owningTenant);
        UUID otherTenant = enabledTenant();

        mockMvc.perform(delete("/tenants/" + otherTenant + "/search/" + PAGE).param("queryJobId", QUERY_ID))
                .andExpect(status().isNotFound());

        verify(trinoClient, never()).cancelQuery(anyString(), anyMap());
        assertThat(queryJobIn(owningTenant)).get()
                .as("the owning tenant's query job after another tenant tried to cancel it")
                .extracting(QueryJob::getFinishedAt)
                .isNull();
    }

    @Test
    public void getNextPage_should_serveTheQueryJobOfTheTenantThatOwnsIt() throws Exception {
        UUID tenantId = enabledTenant();
        queryJobOf(tenantId);

        mockMvc.perform(get("/tenants/" + tenantId + "/search/" + PAGE).param("queryJobId", QUERY_ID))
                .andExpect(status().isOk());
    }

    @Test
    public void request_should_returnNotFound_when_theTenantIsUnknown() throws Exception {
        mockMvc.perform(post("/tenants/" + UUID.randomUUID() + "/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"SELECT 1\"}"))
                .andExpect(status().isNotFound());

        verify(trinoClient, never()).query(anyString(), anyMap());
    }

    @Test
    public void request_should_returnNotFound_when_theTenantIsDisabled() throws Exception {
        mockMvc.perform(post("/tenants/" + disabledTenant() + "/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"SELECT 1\"}"))
                .andExpect(status().isNotFound());

        verify(trinoClient, never()).query(anyString(), anyMap());
    }

    @Test
    public void request_should_returnNotFound_when_theTenantIsNotAUuid() throws Exception {
        mockMvc.perform(get("/tenants/not-a-uuid/search/" + PAGE).param("queryJobId", QUERY_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    public void legacyPath_should_actAsTheManagementTenant() throws Exception {
        jdbi.useExtension(QueryJobDao.class, dao -> dao.create(QueryJob.builder()
                .id(QUERY_ID)
                .tenantId(TenantId.MANAGEMENT.getValue())
                .query("SELECT 1")
                .startedAt(Instant.now())
                .lastActivityAt(Instant.now())
                .nextPageUrl("http://trino.example.com/" + PAGE)
                .build()));

        mockMvc.perform(get("/search/" + PAGE).param("queryJobId", QUERY_ID))
                .andExpect(status().isOk());
    }

    @Test
    public void tenantDeletion_should_purgeTheTenantsQueryJobs() {
        UUID deletedTenant = enabledTenant();
        queryJobOf(deletedTenant);

        lifecycleHandler.onTenantDeleted(deletedTenant);

        assertThat(queryJobIn(deletedTenant)).as("the deleted tenant's query job").isEmpty();
        verify(trinoClient).killQuery(any());
    }

    @Test
    public void tenantDeletion_shouldNot_purgeAnotherTenantsQueryJobs() {
        UUID survivingTenant = enabledTenant();
        queryJobOf(survivingTenant);

        lifecycleHandler.onTenantDeleted(enabledTenant());

        assertThat(queryJobIn(survivingTenant)).as("the surviving tenant's query job").isPresent();
    }

    @Test
    public void search_should_recordTheRequestTenantOnTheQueryJob() {
        UUID tenantId = enabledTenant();

        tenantContextAccessor.runAs(tenantId, () -> queryJobOf(tenantId));

        assertThat(queryJobIn(tenantId)).get()
                .as("the query job created within a tenant")
                .extracting(QueryJob::getTenantId)
                .isEqualTo(tenantId);
    }
}
