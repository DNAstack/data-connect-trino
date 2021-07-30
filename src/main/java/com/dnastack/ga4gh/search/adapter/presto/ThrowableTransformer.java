package com.dnastack.ga4gh.search.adapter.presto;

import com.dnastack.ga4gh.search.adapter.presto.exception.*;
import com.dnastack.ga4gh.search.adapter.shared.AuthRequiredException;
import com.dnastack.ga4gh.search.adapter.shared.SearchAuthRequest;
import com.dnastack.ga4gh.search.model.TableError;
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
        entry(PrestoNoSuchCatalogException.class, HttpStatus.NOT_FOUND),
        entry(PrestoNoSuchSchemaException.class, HttpStatus.NOT_FOUND),
        entry(PrestoNoSuchTableException.class, HttpStatus.NOT_FOUND),
        entry(PrestoBadlyQualifiedNameException.class, HttpStatus.NOT_FOUND),
        entry(PrestoNoSuchColumnException.class, HttpStatus.BAD_REQUEST),
        entry(PrestoInvalidQueryException.class, HttpStatus.BAD_REQUEST),
        entry(PrestoInsufficientResourcesException.class, HttpStatus.SERVICE_UNAVAILABLE),
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
            SearchAuthRequest searchAuthRequest = ((AuthRequiredException) throwable).getAuthorizationRequest();
            error.setTitle("Authentication Required");
            error.setSource(searchAuthRequest.getKey());
            error.setDetails("User is not authorized to access catalog: " + searchAuthRequest.getKey()
                + ", request requires additional authorization information");
        } else if (throwable instanceof PrestoUnexpectedHttpResponseException || throwable instanceof PrestoIOException) {
            error.setTitle(throwable.getMessage());
        } else if (throwable instanceof PrestoErrorException) {
            handlePrestoError(error, (PrestoErrorException) throwable);
        } else if (throwable instanceof PrestoBadlyQualifiedNameException) {
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

    private void handlePrestoError(TableError error, PrestoErrorException prestoErrorException) {
        var prestoError = prestoErrorException.getPrestoError();
        error.setTitle(prestoError.getMessage());
        error.setDetails(String.format(
            "%s: %s: %s",
            prestoError.getErrorType(),
            prestoError.getErrorCode(),
            prestoError.getErrorName()
        ));
    }
}
