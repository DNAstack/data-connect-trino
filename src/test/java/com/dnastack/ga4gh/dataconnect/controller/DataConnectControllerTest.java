package com.dnastack.ga4gh.dataconnect.controller;

import com.dnastack.tenancy.context.TenantIdentitySource;
import org.junit.Test;

import static com.dnastack.ga4gh.dataconnect.controller.DataConnectController.relayedPagePath;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reading the Trino page out of a {@code /search/**} path. Two kinds of path are asked about, and they spell
 * this service's own prefix differently: a request URI reaches the servlet with any proxy prefix stripped,
 * while the path of a page URL this service generated carries {@code X-Forwarded-Prefix} put back on. Both
 * have to yield the same page, because a page read from one is relayed to Trino alongside pages read from the
 * other.
 */
public class DataConnectControllerTest {

    /** The page as Trino issues it: a statement URI's path, with no segment named "search". */
    private static final String PAGE =
        "v1/statement/executing/20260909_120000_00001_abcde/y5bb5cace5500a2cf109b1c50c648b009c40a142f/1";

    private static final String TENANT = "8e5f2a1c-0d3b-4e6a-9c7f-1b2d3e4f5a6b";

    /**
     * Every tenant-scoped request mapping in this service writes "/tenants/{tenantId}" to optimize for readability
     * and to make it easy to grep for. This test ensures that the tenancy library's constant matches that inlined
     * name, so the library can read the tenant from the request path.
     */
    @Test
    public void tenantLibraryPathVariable_should_matchTheInlinedTenantIdNameWeUse() {
        assertThat(TenantIdentitySource.TENANT_PATH_VARIABLE)
            .as("the tenancy library's TENANT_PATH_VARIABLE")
            .isEqualTo("tenantId");
    }

    @Test
    public void relayedPagePath_should_readThePage_when_requestPathHasNoTenant() {
        assertThat(relayedPagePath("/search/" + PAGE))
            .as("the page read from a request URI")
            .isEqualTo(PAGE);
    }

    @Test
    public void relayedPagePath_should_readThePage_when_requestPathHasATenant() {
        assertThat(relayedPagePath("/tenants/" + TENANT + "/search/" + PAGE))
            .as("the page read from a tenant-addressed request URI")
            .isEqualTo(PAGE);
    }

    @Test
    public void relayedPagePath_should_readThePage_when_givenAGeneratedUrlBehindAProxyPrefix() {
        // The path of the nextPageUrl this service hands back when the caller reached it through a gateway
        // that set X-Forwarded-Prefix. The POST /search fast path re-reads its own generated URL, so a prefix
        // here must not leave the page empty and send the follow-up request to Trino's root.
        assertThat(relayedPagePath("/api/data-connect/search/" + PAGE))
            .as("the page read from a generated URL carrying a proxy prefix")
            .isEqualTo(PAGE);
    }

    @Test
    public void relayedPagePath_should_readThePage_when_aGeneratedUrlCarriesBothAProxyPrefixAndATenant() {
        assertThat(relayedPagePath("/api/data-connect/tenants/" + TENANT + "/search/" + PAGE))
            .as("the page read from a generated URL carrying both a proxy prefix and a tenant")
            .isEqualTo(PAGE);
    }

    @Test
    public void relayedPagePath_should_readThePage_when_theServiceIsMountedUnderAContextPath() {
        assertThat(relayedPagePath("/data-connect-trino/search/" + PAGE))
            .as("the page read from a request URI carrying a servlet context path")
            .isEqualTo(PAGE);
    }

    @Test
    public void relayedPagePath_should_readNoPage_when_thePathNamesNone() {
        assertThat(relayedPagePath("/search"))
            .as("the page read from a path that stops short of naming one")
            .isEmpty();
    }

    @Test
    public void relayedPagePath_should_readNoPage_when_thePathEndsAtTheSearchSegment() {
        assertThat(relayedPagePath("/search/"))
            .as("the page read from a path ending at the search segment")
            .isEmpty();
    }
}
