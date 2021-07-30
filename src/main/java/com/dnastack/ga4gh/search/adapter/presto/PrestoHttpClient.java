package com.dnastack.ga4gh.search.adapter.presto;

import brave.Span;
import brave.Tracer;
import com.dnastack.ga4gh.search.adapter.presto.exception.PrestoIOException;
import com.dnastack.ga4gh.search.adapter.presto.exception.PrestoUnexpectedHttpResponseException;
import com.dnastack.ga4gh.search.adapter.security.ServiceAccountAuthenticator;
import com.dnastack.ga4gh.search.adapter.shared.AuthRequiredException;
import com.dnastack.ga4gh.search.adapter.shared.SearchAuthRequest;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

@Slf4j
public class PrestoHttpClient implements PrestoClient {

    private static final TypeReference<Map<String, String>> MAP_TYPE_REFERENCE = new TypeReference<>() {
    };

    private static final String DEFAULT_PRESTO_USER_NAME = "ga4gh-search-adapter-presto";

    private final String prestoServer;
    private final String prestoSearchEndpoint;
    private final ServiceAccountAuthenticator authenticator;
    private final OkHttpClient httpClient;
    private final Tracer tracer;
    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public PrestoHttpClient(Tracer tracer,OkHttpClient httpClient,String prestoServerUrl, ServiceAccountAuthenticator accountAuthenticator) {
        this.prestoServer = prestoServerUrl;
        this.prestoSearchEndpoint = prestoServerUrl + "/v1/statement";
        this.authenticator = accountAuthenticator;
        this.tracer = tracer;
        this.httpClient = httpClient;
    }

    public JsonNode query(String statement, Map<String, String> extraCredentials) {
        Span span = tracer.nextSpan().name("prestoQuery");
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span.start())) {
                log.debug("Posting to " + prestoSearchEndpoint);
            try (Response response = post(prestoSearchEndpoint, statement, extraCredentials)) {
                log.debug("Got response, now polling for query results");
                JsonNode jn = getQueryResults(response);
                return jn;
            } catch (final AuthRequiredException e) {
                log.debug("Passing back auth challenge from backend: " + e.getAuthorizationRequest());
                throw e;
            } catch (final IOException e) {
                throw new PrestoIOException("Unable to initiate search (I/O error).", e);
            }
        } finally {
            span.finish();
        }

    }

//    public Single<JsonNode> next(String page, Map<String, String> extraCredentials) {
//        return Single.defer(() -> {
//            return Single.fromCallable(() -> {
//                //TODO: better url construction
//                try (Response response = get(this.prestoServer + "/" + page, extraCredentials)) {
//                    return pollForQueryResults(response, extraCredentials, 0, new QueryManager());
//                }
//            });
//        }).subscribeOn(Schedulers.io());
//    }

    public JsonNode next(String page, Map<String, String> extraCredentials) {
        Span span = tracer.nextSpan().name("prestoNext");
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span.start())) {
            //TODO: better url construction
            String url = page.startsWith("/") ? this.prestoServer + page : this.prestoServer + "/" + page;

            try (Response response = get(url, extraCredentials)) {
                return getQueryResults(response);
            } catch (IOException ie) {
                throw new PrestoIOException("Unable to fetch more search or listing results (I/O error).", ie);
            }
        } finally {
            span.finish();
        }
    }

    private SearchAuthRequest extractExtraCredentialsRequest(JsonNode node) {
        JsonNode error = node.get("error");
        if (error != null &&
            Objects.equals(error.get("errorName").asText(), "RESOURCE_AUTH_REQUIRED")) {

            PrestoFailureInfo failureInfo = objectMapper
                .convertValue(error.get("failureInfo"), PrestoFailureInfo.class);
            String embeddedJson = failureInfo.getMessageOfCauseType("io.prestosql.spi.PrestoException");
            if (embeddedJson == null) {
                throw new RuntimeException(
                    "This looks like an auth challenge from Presto, but the auth challenge payload could not be found in "
                        + embeddedJson);
            }
            try {
                Map<String, String> fromPresto = objectMapper.readValue(embeddedJson, MAP_TYPE_REFERENCE);
                String key = requireNonNull(fromPresto.remove("extra-credentials-key"),
                    "Coudn't find extra-credentials-key in auth request from backend");
                String resourceType = requireNonNull(fromPresto.remove("resource-type"),
                    "Coudn't find resource-type in auth request from backend");
                return new SearchAuthRequest(key, resourceType, fromPresto);
            } catch (IOException e) {
                throw new PrestoIOException(
                    "The backend requested additional auth info but it couldn't be parsed as a JSON object: "
                        + embeddedJson, e);
            }
        }
        return null;
    }

    /**
     * Models the structure of the "failureInfo" member that comes back from a Presto error response.
     */
    @Data
    private static class PrestoFailureInfo {

        String type;
        String message;
        PrestoFailureInfo cause;

        /**
         * Digs through the cause chain until it finds a cause with the given type, then returns the message from that
         * node.
         *
         * @param fqcn fully-qualified class name of the exception type whose message to retrieve.
         * @return the message associated with the given exception in the cause chain.
         */
        String getMessageOfCauseType(String fqcn) {
            if (type.equals(fqcn)) {
                return message;
            }
            if (cause == null) {
                return null;
            }
            return cause.getMessageOfCauseType(fqcn);
        }
    }

    /**
     * Returns the value of JSON node {@code stats.state}, or the special string {@code "(no state in response)"} if
     * that node doesn't exist.
     *
     * @param node the node to start the search at (usually the root of the Presto response)
     * @return the state under the given node or the special {@code "(no state in response)"}. Never null.
     */
    private String extractState(JsonNode node) {
        if (node.hasNonNull("stats")) {
            return node.get("stats").get("state").asText();
        }
        return "(no state in response)";
    }


    private boolean isRunning(String prestoState) {
        return !(prestoState.equalsIgnoreCase("FINISHED") ||
               prestoState.equalsIgnoreCase("CLIENT_ABORTED") ||
               prestoState.equalsIgnoreCase("CLIENT_ERROR"));
    }
    private JsonNode getQueryResults(Response httpResponse) throws PrestoUnexpectedHttpResponseException, AuthRequiredException {
        if (httpResponse.body() == null) {
            throw new PrestoUnexpectedHttpResponseException(
                httpResponse.code(),
                "Unable to fetch query results",
                "No response body received from backend - status line was " + httpResponse.code() + " " + httpResponse
                    .message());
        }

        String httpResponseBody;
        try {
            httpResponseBody=httpResponse.body().string();
        } catch (IOException ie) {
            throw new PrestoUnexpectedHttpResponseException(httpResponse.code(), "Failed to complete database query: problem with response", "IOException while extracting http response body from Presto", ie);
        }

        if (!httpResponse.isSuccessful()) {
            throw new PrestoUnexpectedHttpResponseException(httpResponse.code(), "An error occurred during query execution", "Presto Http response was not successful: " + httpResponseBody);
        }

        try {
            JsonNode jsonBody = objectMapper.readTree(httpResponseBody); //ioexception never happens;
            String prestoState = extractState(jsonBody);

            if (isRunning(prestoState) || prestoState.equalsIgnoreCase("finished")) {
                log.trace("PrestoState: {}", prestoState);
                log.trace("Presto Results: {}", jsonBody.toString());
                assert (jsonBody.hasNonNull("columns"));
                return jsonBody;
            } else {

                SearchAuthRequest credentialsRequest = extractExtraCredentialsRequest(jsonBody);
                if (credentialsRequest != null) {
                    throw new AuthRequiredException(credentialsRequest);
                }

                Object error = jsonBody.get("error");
                if (error == null) {
                    error = httpResponseBody;
                }
                throw new PrestoUnexpectedHttpResponseException(httpResponse.code(), "An errr ocurred during query execution.", "Processing failure, prestoState:" + prestoState + " -- " + error.toString());
            }
        } catch (JsonParseException jpe) {
            throw new PrestoUnexpectedHttpResponseException(httpResponse.code(), "An errr ocurred during query execution.", "Error while parsing JSON response from database", jpe);
        } catch (IOException iex) {
            // should never happen: no I/O is performed by objectMapper.readTree
            throw new AssertionError("IOException when parsing http body string response.");
        } finally {
            httpResponse.close();
        }

    }

    private Response get(String url, Map<String, String> extraCredentials) throws IOException {
        Request.Builder request = new Request.Builder().url(url).method("GET", null);
        return execute(request, extraCredentials);
    }

    private Response post(String url, String body, Map<String, String> extraCredentials) throws IOException {
        RequestBody requestBody = RequestBody.create(null, body);
        Request.Builder request = new Request.Builder().url(url).method("POST", requestBody);
        return execute(request, extraCredentials);
    }

    private Response execute(final Request.Builder request, Map<String, String> extraCredentials) throws IOException {
        request.header("X-Presto-User", getUserNameForRequest());
        extraCredentials.forEach((k, v) -> request.addHeader("X-Presto-Extra-Credential", k + "=" + v));

        if (!authenticator.requiresAuthentication()) {
            Request r = request.build();
            log.info(">>> {} {} (No Authorization header, {} extra credentials)", r.method(), r.url(), extraCredentials
                .size());
            Response response = httpClient.newCall(r).execute();
            log.info("GET "+r.url()+" returned "+response.code());
            if(response !=null && !response.isSuccessful()){
                log.debug("GET "+r.url()+" gave unsuccessful response "+response.code()+": "+
                          ((response.body()==null) ? "null" : response.body().string()));
            }
            return response;
        }

        request.header("Authorization", "Bearer " + authenticator.getAccessToken());
        Request r = request.build();
        log.debug(">>> {} {} (With Authorization header, {} extra credentials)", r.method(), r.url(), extraCredentials
            .size());

        Response firstTry = httpClient.newCall(r).execute();
        if (firstTry.code() != 401 && firstTry.code() != 403) {
            return firstTry;
        }

        log.debug("Got {}. Will refresh access token and retry.", firstTry.code());
        firstTry.close();
        authenticator.refreshAccessToken();
        request.header("Authorization", "Bearer " + authenticator.getAccessToken());
        r = request.build();
        log.debug(">>> {} {} (With refreshed Authorization header, {} extra credentials)", r.method(), r
            .url(), extraCredentials.size());
        return httpClient.newCall(r).execute();
    }

    /**
     * If the Incoming request has authentication information, use the attached user principal as the username to pass
     * to presto, otherwise, return {@link #DEFAULT_PRESTO_USER_NAME the default username}.
     */
    private String getUserNameForRequest() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return DEFAULT_PRESTO_USER_NAME;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return ((User) principal).getUsername();
        }
        if (principal instanceof Jwt) {
            return ((Jwt) principal).getSubject();
        }

        return principal.toString();
    }
}
