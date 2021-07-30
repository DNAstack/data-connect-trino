package com.dnastack.ga4gh.search.adapter;

import com.dnastack.ga4gh.search.adapter.matchers.IsUrl;
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
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static io.restassured.RestAssured.given;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.junit.Assert.fail;

@Slf4j
public abstract class BaseE2eTest {
    protected static final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    protected static Boolean useSSL;
    protected static RestAssuredConfig config;
    protected static String walletClientId = optionalEnv("E2E_WALLET_CLIENT_ID", "ga4gh-search-adapter-presto-e2e-test");
    protected static String walletClientSecret = optionalEnv("E2E_WALLET_CLIENT_SECRET", "dev-secret-never-use-in-prod");
    protected static String searchAdapterAudience = optionalEnv("E2E_WALLET_AUDIENCE", null);

    protected List<Runnable> cleanupOperations = new LinkedList<>();

    @After
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

    @BeforeClass
    public static void setupRestAssured() {
        RestAssured.baseURI = requiredEnv("E2E_BASE_URI");
        RestAssured.replaceFiltersWith(new TraceLoggingFilter());
        try {
            if (new URI(RestAssured.baseURI).getHost().equalsIgnoreCase("localhost")) {
                log.info("E2E BASE URI is at localhost, allowing localhost to occur within URLs of JSON responses.");
                IsUrl.setAllowLocalhost(true);
                useSSL=false;
            } else {
                useSSL=true;
            }
        } catch (URISyntaxException use) {
            throw new RuntimeException(String.format("Error initializing tests -- E2E_BASE_URI (%s) is invalid", RestAssured.baseURI));
        }
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

    @BeforeClass
    public static void setupObjectMapper() {
        config = RestAssuredConfig.config()
            .objectMapperConfig(
                ObjectMapperConfig.objectMapperConfig()
                    .defaultObjectMapperType(ObjectMapperType.JACKSON_2)
                    .jackson2ObjectMapperFactory((cls, charset) -> objectMapper)
            );
    }

    protected static String requiredEnv(String name) {
        String val = System.getenv(name);
        if (val == null) {
            fail("Environnment variable `" + name + "` is required");
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

    interface ExceptionalSupplier<T, E extends Exception> {
        T get() throws E;
    }

    protected static <E extends Exception> String lazyOptionalEnv(String name, ExceptionalSupplier<String, E> defaultValue) throws E {
        String val = System.getenv(name);
        if (val == null) {
            return defaultValue.get();
        }
        return val;
    }

    static String getToken(String audience, String... scopes) {
        RequestSpecification specification = new RequestSpecBuilder().setBaseUri(optionalEnv("E2E_WALLET_TOKEN_URI", "http://localhost:8081/oauth/token"))
            .build();

        //@formatter:off
        RequestSpecification requestSpecification = given(specification)
            .config(config)
            .auth().basic(walletClientId, walletClientSecret)
            .formParam("grant_type", "client_credentials")
            .formParam("client_id", walletClientId)
            .formParam("client_secret", walletClientSecret)
            .formParam("resource", audience + "/");
        if (scopes.length > 0) {
            requestSpecification.formParam("scope", String.join(" ", scopes));
        }
        JsonPath tokenResponse = requestSpecification
            .when()
            .post()
            .then()
            .log().ifValidationFails()
            .statusCode(200)
            .extract().jsonPath();
        //@formatter:on

        return tokenResponse.getString("access_token");
    }
}
