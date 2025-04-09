package com.dnastack.ga4gh.dataconnect.adapter.shared;

import brave.Tracer;
import com.dnastack.ga4gh.dataconnect.adapter.trino.exception.TableApiErrorException;
import com.dnastack.ga4gh.dataconnect.model.TableError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
            .body(Map.of("authorization-request", cr, "trace_id", tracer.currentSpan().context().traceIdString()));
    }

    @ExceptionHandler({TableApiErrorException.class})
    public ResponseEntity<?> handleTableApiErrorException(TableApiErrorException throwable) {
        String traceId = tracer.currentSpan().context().traceIdString();
        TableError error = TableError.fromThrowable(throwable.getCause(), null);
        log.error("Generating response with error that escaped controller: {}", error);

        if (traceId != null) {
            error.setDetails(traceId + ": " + error.getDetails());
        }

        Object body = throwable.getResponseBodyGenerator().apply(error);

        return ResponseEntity.status(error.getStatus())
            .body(body);
    }

    private static String escapeQuotes(String s) {
        return s.replace("\"", "\\\"");
    }

}
