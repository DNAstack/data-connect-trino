package com.dnastack.ga4gh.dataconnect.adapter;

import com.dnastack.ga4gh.dataconnect.adapter.matchers.IsUrl;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static io.restassured.RestAssured.given;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
public abstract class BaseE2eTest {
    protected static final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    protected static String walletClientId = optionalEnv("E2E_WALLET_CLIENT_ID", "data-connect-trino-e2e-test");
    protected static String walletClientSecret = optionalEnv("E2E_WALLET_CLIENT_SECRET", "dev-secret-never-use-in-prod");
    protected static String dataConnectAdapterResource = optionalEnv("E2E_WALLET_RESOURCE", "http://localhost:8089/");
    protected static String walletTokenUrl = optionalEnv("E2E_WALLET_TOKEN_URI", "http://localhost:8081/oauth/token");

    protected List<Runnable> cleanupOperations = new LinkedList<>();

    @AfterEach
    public void runAdditionalCleanupOperations() {
        Collections.reverse(cleanupOperations);
        for (int i = 0, n = cleanupOperations.size(); i < n; i++) {
            try {
                cleanupOperations.get(i).run();
            } catch (Exception e) {
                log.warn("Cleanup operation " + i + "/" + n + " failed. Continuing with subsequent cleanups.", e);
            }
        }
        cleanupOperations = new LinkedList<>();
    }

    /**
     * Adds an operation to the cleanupOperations list which is then executed after a test is run. Should be used if
     * your test creates any test data, so that it is reset between test runs and doesn't end up affecting another test.
     *
     * @param runnable the lambda expression to cleanup the test data that was created by a test.
     */
    protected void afterThisTest(Runnable runnable) {
        cleanupOperations.add(runnable);
    }

    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.baseURI = optionalEnv("E2E_BASE_URI", "http://localhost:8089");
        RestAssured.replaceFiltersWith(new TraceLoggingFilter());
        RestAssured.config = RestAssured.config()
                .objectMapperConfig(
                        ObjectMapperConfig.objectMapperConfig()
                                .jackson2ObjectMapperFactory((type, s) -> objectMapper)
                );
        try {
            if (new URI(RestAssured.baseURI).getHost().equalsIgnoreCase("localhost")) {
                log.info("E2E BASE URI is at localhost, allowing localhost to occur within URLs of JSON responses.");
                IsUrl.setAllowLocalhost(true);
            }
        } catch (URISyntaxException use) {
            throw new RuntimeException(String.format("Error initializing tests -- E2E_BASE_URI (%s) is invalid", RestAssured.baseURI));
        }
    }

    @BeforeEach
    public void printTestStartMessage(TestInfo testInfo) {
        log.info("*** Starting test case {}", testInfo.getDisplayName());
    }

    private static class TraceLoggingFilter implements Filter {

        private static final Random random = new Random();

        @Override
        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
            String traceId = String.format("%016x", random.nextLong());
            requestSpec.replaceHeader("X-B3-TraceId", traceId);
            requestSpec.replaceHeader("X-B3-SpanId", traceId);
            log.info(">>> [{}] {} {}", traceId, requestSpec.getMethod(), requestSpec.getURI());

            long sendTime = System.currentTimeMillis();
            try {
                Response response = ctx.next(requestSpec, responseSpec);

                log.info("<<< [{}] {} {} ({}ms)",
                    traceId,
                    isBlank(response.getStatusLine()) ? "(no response status)" : response.getStatusLine().trim(),
                    isBlank(response.getContentType()) ? "(no content type)" : response.getContentType(),
                    response.getTime());
                return response;
            } catch (final Exception e) {
                long elapsedTime = System.currentTimeMillis() - sendTime;
                // logging e.toString() here rather than whole exception because the test runner will log the rethrown exception
                log.info("<<< [{}] {} ({}ms)", traceId, e.toString(), elapsedTime);
                throw e;
            }
        }
    }

    protected static String requiredEnv(String name) {
        String val = System.getenv(name);
        if (val == null) {
            Assertions.fail("Environment variable `" + name + "` is required");
        }
        return val;
    }

    protected static String optionalEnv(String name, String defaultValue) {
        String val = System.getenv(name);
        if (val == null) {
            return defaultValue;
        }
        return val;
    }

    static String getToken(List<String> scopes, List<String> resources) {
        RequestSpecification specification = new RequestSpecBuilder().setBaseUri(walletTokenUrl)
            .build();

        RequestSpecification requestSpecification = given(specification)
            .auth().basic(walletClientId, walletClientSecret)
            .formParam("grant_type", "client_credentials")
            .formParam("client_id", walletClientId)
            .formParam("client_secret", walletClientSecret);

        if (!scopes.isEmpty()) {
            requestSpecification.formParam("scope", String.join(" ", scopes));
        }

        if (!resources.isEmpty()) {
            for (String resource : resources) {
                requestSpecification.formParam("resource", resource);
            }
        }

        JsonPath tokenResponse = requestSpecification
            .when()
            .post()
            .then()
            .log().ifValidationFails()
            .statusCode(200)
            .extract().jsonPath();

        return tokenResponse.getString("access_token");
    }
}
