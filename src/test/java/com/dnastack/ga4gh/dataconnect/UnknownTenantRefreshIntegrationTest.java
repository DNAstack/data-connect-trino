package com.dnastack.ga4gh.dataconnect;

import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoClient;
import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoDataPage;
import com.dnastack.ga4gh.dataconnect.repository.QueryJob;
import com.dnastack.ga4gh.dataconnect.repository.QueryJobDao;
import com.dnastack.ga4gh.dataconnect.tenancy.TenantMirrorLifecycleHandler;
import com.dnastack.tenancy.context.UnknownTenantRefresh;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.EMBEDDED;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A request for a tenant this service has not mirrored yet, with a refresh standing in for the tenant lifecycle's:
 * the request is served when the refresh brings the tenant in, and refused as unknown when it does not.
 */
@AutoConfigureEmbeddedDatabase(provider = EMBEDDED, refresh = AFTER_EACH_TEST_METHOD, type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = DataConnectTrinoApplication.class,
        properties = {"management.tracing.enabled=false", "tenant-lifecycle.enabled=false"}
)
@AutoConfigureMockMvc
@ActiveProfiles("no-auth")
public class UnknownTenantRefreshIntegrationTest {

    private static final String QUERY_ID = "20260903_120000_00001_abcde";
    private static final String PAGE = "v1/statement/executing/" + QUERY_ID + "/y5bb5cace5500a2cf109b1c50c648b009c40a142f/2";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Jdbi jdbi;

    @Autowired
    private TenantMirrorLifecycleHandler lifecycleHandler;

    @MockitoBean
    private UnknownTenantRefresh tenantRefresh;

    @MockitoBean
    private TrinoClient trinoClient;

    @Before
    public void stubTrino() {
        when(trinoClient.next(anyString(), anyMap()))
                .thenReturn(new TrinoDataPage(QUERY_ID, null, null, null, null, null, null));
    }

    @Test
    public void getNextPage_should_serveTheTenant_when_theRefreshBringsItIn() throws Exception {
        UUID created = UUID.randomUUID();
        doAnswer(invocation -> {
            lifecycleHandler.onTenantEnabled(
                    TenantSnapshot.builder().id(created).name("tenant-" + created).status(TenantStatus.ENABLED).build());
            return null;
        }).when(tenantRefresh).refresh();
        Instant now = Instant.now();
        jdbi.useExtension(QueryJobDao.class, dao -> dao.create(QueryJob.builder()
                .id(QUERY_ID)
                .tenantId(created)
                .query("SELECT 1")
                .startedAt(now)
                .lastActivityAt(now)
                .nextPageUrl("http://trino.example.com/" + PAGE)
                .build()));

        mockMvc.perform(get("/tenants/" + created + "/search/" + PAGE).param("queryJobId", QUERY_ID))
                .andExpect(status().isOk());
    }

    @Test
    public void request_should_answerNotFound_when_theTenantIsStillUnknownAfterRefreshing() throws Exception {
        mockMvc.perform(get("/tenants/" + UUID.randomUUID() + "/search/" + PAGE).param("queryJobId", QUERY_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].type").value("urn:dnastack:tenancy:unknown-tenant"));
        verify(tenantRefresh).refresh();
    }
}
