package com.dnastack.ga4gh.dataconnect.adapter.shared;

import io.micrometer.tracing.Tracer;
import com.dnastack.ga4gh.dataconnect.adapter.trino.exception.TableApiErrorException;
import com.dnastack.ga4gh.dataconnect.adapter.trino.exception.TrinoUnknownTenantException;
import com.dnastack.ga4gh.dataconnect.model.DataConnectErrorResponse;
import com.dnastack.ga4gh.dataconnect.model.TableError;
import com.dnastack.tenancy.context.UnknownTenantException;
import com.dnastack.tenancy.model.Tenants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalControllerExceptionHandler {
    @Autowired
    private Tracer tracer;

    @ExceptionHandler(AuthRequiredException.class)
    public ResponseEntity<?> handleAuthRequiredException(AuthRequiredException e) {
        DataConnectAuthRequest cr = e.getAuthorizationRequest();
        return ResponseEntity.status(401)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .header("WWW-Authenticate", "GA4GH-Search realm=\"" + escapeQuotes(cr.getKey()) + "\"")
            .body(Map.of("authorization-request", cr, "trace_id", tracer.currentSpan().context().traceId()));
    }

    @ExceptionHandler({TableApiErrorException.class})
    public ResponseEntity<?> handleTableApiErrorException(TableApiErrorException throwable) {
        String traceId = tracer.currentSpan().context().traceId();
        TableError error = TableError.fromThrowable(throwable.getCause(), null);
        if (isClientError(error)) {
            // The fault is in the request, so there is no stack trace here worth reading, and nothing for an
            // error dashboard to count: a caller sending a bad request is this service working as intended.
            log.info("Answering {} to a request this service will not serve: {}", error.getStatus(), error);
        } else if (throwable.getCause() instanceof TrinoUnknownTenantException) {
            // Expected for a poll interval after a tenant is created, and passes on its own: worth seeing in the log,
            // but with no stack trace to read and no error for a dashboard to count.
            log.warn("Answering {} while a service this query depends on hasn't learned of the tenant: {}",
                error.getStatus(), error);
        } else {
            log.error("Generating response with error that escaped controller: {}", error, throwable);
        }

        if (traceId != null) {
            error.setDetails(traceId + ": " + error.getDetails());
        }

        return ResponseEntity.status(error.getStatus())
            .body(DataConnectErrorResponse.of(error));
    }

    /**
     * In Data Connect's error format, so a caller reading {@code errors} can tell a tenant this service hasn't learned
     * of yet from any other 404.
     */
    @ExceptionHandler(UnknownTenantException.class)
    public ResponseEntity<DataConnectErrorResponse> handleUnknownTenantException(UnknownTenantException e) {
        TableError error = new TableError();
        error.setStatus(HttpStatus.NOT_FOUND.value());
        error.setTitle(e.getMessage());
        error.setType(Tenants.UNKNOWN_TENANT_PROBLEM_TYPE.toString());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(DataConnectErrorResponse.of(error));
    }

    /** Whether the status blames the caller rather than this service. */
    private static boolean isClientError(TableError error) {
        return HttpStatus.resolve(error.getStatus()) != null
            && HttpStatus.valueOf(error.getStatus()).is4xxClientError();
    }

    private static String escapeQuotes(String s) {
        return s.replace("\"", "\\\"");
    }

}
