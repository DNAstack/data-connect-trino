package com.dnastack.ga4gh.search.adapter;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;
import static java.util.Collections.emptyList;

public class CorsE2eTest extends BaseE2eTest {

    private List<String> allowedCorsOrigins() {
        String config = optionalEnv("E2E_CORS_URLS", null);
        if (config == null) {
            return emptyList();
        }
        return Arrays.asList(config.split(","));
    }

    @Test
    public void corsRequest_shouldNot_return401() {
        for (String origin : allowedCorsOrigins()) {
            // @formatter:off
            given()
                .header("access-control-request-method", "GET")
                .header("origin", origin)
            .when()
                .options("/tables")
            .then()
                .log().ifValidationFails()
                .statusCode(200);
            // @formatter:on
        }
    }

}
