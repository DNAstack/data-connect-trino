package com.dnastack.ga4gh.search.adapter.shared;

import brave.Tracer;
import com.dnastack.ga4gh.search.adapter.presto.ThrowableTransformer;
import com.dnastack.ga4gh.search.adapter.presto.exception.*;
import com.dnastack.ga4gh.search.model.TableData;
import com.dnastack.ga4gh.search.model.TableError;
import com.dnastack.ga4gh.search.model.TableInfo;
import com.dnastack.ga4gh.search.model.TablesList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalControllerExceptionHandler {
    @Autowired
    private Tracer tracer;

    @Autowired
    private ThrowableTransformer throwableTransformer;

    @ExceptionHandler(AuthRequiredException.class)
    public ResponseEntity<?> handleAuthRequiredException(AuthRequiredException e) {
        SearchAuthRequest cr = e.getAuthorizationRequest();
        return ResponseEntity.status(401)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .header("WWW-Authenticate", "GA4GH-Search realm=\"" + escapeQuotes(cr.getKey()) + "\"")
            .body(Map.of("authorization-request", cr, "trace_id", tracer.currentSpan().context().traceIdString()));
    }

    @ExceptionHandler({TableApiErrorException.class})
    public ResponseEntity<?> handleTableApiErrorException(TableApiErrorException throwable) {
        String traceId = tracer.currentSpan().context().traceIdString();
        TableError error = throwableTransformer.transform(throwable.getPreviousException(), null);

        if (traceId != null) {
            error.setDetails(traceId + ": " + error.getDetails());
        }

        Object body = throwable.getErrorSupplier().apply(error);

        return ResponseEntity.status(throwableTransformer.getResponseStatus(throwable.getPreviousException()))
            .body(body);
    }

    private static String escapeQuotes(String s) {
        return s.replace("\"", "\\\"");
    }

}
