package com.dnastack.ga4gh.dataconnect.adapter.trino;

import com.dnastack.ga4gh.dataconnect.adapter.trino.exception.*;
import com.dnastack.ga4gh.dataconnect.adapter.shared.AuthRequiredException;
import com.dnastack.ga4gh.dataconnect.adapter.shared.DataConnectAuthRequest;
import com.dnastack.ga4gh.dataconnect.model.TableError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.UncheckedIOException;
import java.util.Map;

import static java.util.Map.entry;

@Service
@Slf4j
public class ThrowableTransformer {

    private static final Map<Class<?>, HttpStatus> responseStatuses = Map.ofEntries(
        entry(AuthRequiredException.class, HttpStatus.UNAUTHORIZED),
        entry(TrinoNoSuchCatalogException.class, HttpStatus.NOT_FOUND),
        entry(TrinoNoSuchSchemaException.class, HttpStatus.NOT_FOUND),
        entry(TrinoNoSuchTableException.class, HttpStatus.NOT_FOUND),
        entry(TrinoBadlyQualifiedNameException.class, HttpStatus.NOT_FOUND),
        entry(TrinoNoSuchColumnException.class, HttpStatus.BAD_REQUEST),
        entry(TrinoInvalidQueryException.class, HttpStatus.BAD_REQUEST),
        entry(TrinoInsufficientResourcesException.class, HttpStatus.SERVICE_UNAVAILABLE),
        entry(QueryParsingException.class, HttpStatus.BAD_REQUEST),
        entry(InvalidQueryJobException.class, HttpStatus.BAD_REQUEST),
        entry(UnexpectedQueryResponseException.class, HttpStatus.INTERNAL_SERVER_ERROR));



    public TableError transform(Throwable throwable, String catalogName) {
        TableError error = new TableError();
        error.setTitle("Encountered an unexpected error");
        error.setStatus(getResponseStatus(throwable));
        error.setDetails(throwable.getClass().getName());
        error.setSource(catalogName);

        if (throwable instanceof AuthRequiredException) {
            DataConnectAuthRequest dataConnectAuthRequest = ((AuthRequiredException) throwable).getAuthorizationRequest();
            error.setTitle("Authentication Required");
            error.setSource(dataConnectAuthRequest.getKey());
            error.setDetails("User is not authorized to access catalog: " + dataConnectAuthRequest.getKey()
                + ", request requires additional authorization information");
        } else if (throwable instanceof TrinoUnexpectedHttpResponseException || throwable instanceof TrinoIOException) {
            error.setTitle(throwable.getMessage());
        } else if (throwable instanceof TrinoErrorException) {
            handleTrinoError(error, (TrinoErrorException) throwable);
        } else if (throwable instanceof TrinoBadlyQualifiedNameException) {
            error.setTitle(throwable.getMessage());
        } else if (throwable instanceof InvalidQueryJobException) {
            error.setTitle("The query corresponding to this search could not be found (" + ((InvalidQueryJobException) throwable).getQueryJobId() + ")");
        } else if (throwable instanceof QueryParsingException) {
            error.setTitle("Unable to parse query");
        } else if (throwable instanceof UncheckedIOException) {
            error.setTitle("Unchecked IO Error");
            error.setDetails(throwable.getMessage());
        } else if (throwable instanceof UnexpectedQueryResponseException){
            error.setTitle("Unexpected query response");
            error.setDetails(throwable.getMessage());
        }

        log.error(throwable.getMessage(), throwable);
        return error;
    }

    public int getResponseStatus(Throwable throwable) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (responseStatuses.containsKey(throwable.getClass())) {
            status = responseStatuses.get(throwable.getClass());
        }

        return status.value();
    }

    private void handleTrinoError(TableError error, TrinoErrorException trinoErrorException) {
        var trinoError = trinoErrorException.getTrinoError();
        error.setTitle(trinoError.getMessage());
        error.setDetails(String.format(
            "%s: %s: %s",
            trinoError.getErrorType(),
            trinoError.getErrorCode(),
            trinoError.getErrorName()
        ));
    }
}
