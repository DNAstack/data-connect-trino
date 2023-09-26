package com.dnastack.ga4gh.dataconnect.model;

import com.dnastack.ga4gh.dataconnect.adapter.shared.AuthRequiredException;
import com.dnastack.ga4gh.dataconnect.adapter.shared.DataConnectAuthRequest;
import com.dnastack.ga4gh.dataconnect.adapter.trino.exception.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.io.UncheckedIOException;

@Slf4j
@Data
public class TableError {
    private String source;
    private int status; // HTTP status
    private String title;
    private String details;

    public static TableError fromThrowable(Throwable throwable, String catalogName) {
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
        } else if (throwable instanceof TrinoBadlyQualifiedNameException) {
            error.setTitle(throwable.getMessage());
        } else if (throwable instanceof TrinoErrorException) {
            error.setTitle("Trino error");
            error.setDetails(throwable.getMessage());
        } else if (throwable instanceof InvalidQueryJobException) {
            error.setTitle("The query corresponding to this search could not be found (" + ((InvalidQueryJobException) throwable).getQueryJobId() + ")");
        } else if (throwable instanceof QueryParsingException) {
            error.setTitle("Unable to parse query");
        } else if (throwable instanceof UncheckedIOException) {
            error.setTitle("IO Error");
            error.setDetails(throwable.getMessage());
        } else if (throwable instanceof UnexpectedQueryResponseException) {
            error.setTitle("Unexpected query response");
            error.setDetails(throwable.getMessage());
        }

        log.debug("{}", error.getTitle(), throwable);
        return error;
    }

    private static int getResponseStatus(Throwable throwable) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (throwable instanceof HasHttpStatus) {
            status = ((HasHttpStatus) throwable).httpStatus();
        }

        return status.value();
    }
}
