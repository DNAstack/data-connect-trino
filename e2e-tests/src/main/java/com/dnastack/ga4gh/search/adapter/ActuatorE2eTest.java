package com.dnastack.ga4gh.search.adapter;

import io.restassured.RestAssured;
import org.junit.Test;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assume.assumeThat;

public class ActuatorE2eTest extends BaseE2eTest {

    @Test
    public void appNameAndVersionShouldBeExposed() {
        assumeThat(RestAssured.baseURI, not(startsWith("http://localhost")));

        // @formatter:off
        given()
        .when()
            .get("/actuator/info")
        .then()
            .log().ifValidationFails()
            .statusCode(200)
            .body("build.name", equalTo("GA4GH Discovery Search API"))
            .body("build.version", notNullValue());
        // @formatter:on
    }

    @Test
    public void sensitiveInfoShouldNotBeExposed() {
        Stream.of(
                        "auditevents",
                        "beans",
                        "conditions",
                        "configprops",
                        "env",
                        "flyway",
                        "httptrace",
                        "logfile",
                        "loggers",
                        "liquibase",
                        "metrics",
                        "mappings",
                        "prometheus",
                        "scheduledtasks",
                        "sessions",
                        "shutdown",
                        "threaddump")
                .forEach(endpoint -> {
                            given()
                            .when()
                                .get("/actuator/" + endpoint)
                            .then()
                                .log().ifValidationFails()
                                .statusCode(anyOf(equalTo(401), equalTo(404)));
                        });
    }
}
