package com.dnastack.ga4gh.dataconnect.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * The body this service answers with when a request fails outright, whichever endpoint it was addressed to.
 * <p>
 * One body serves all of them because of how Data Connect carries errors. The spec does not define an error
 * envelope of its own: errors are a field of the resource the endpoint returns, so each of this service's three
 * response types declares its own — {@link TableData#getErrors()} for {@code /search} and
 * {@code /table/{name}/data}, {@link TablesList#getErrors()} for {@code /tables} and its per-schema pages, and
 * {@link TableInfo#getErrors()} for {@code /table/{name}/info}. All three declare it under the same JSON name,
 * {@code errors}, holding the same {@link TableError}. A body carrying that one field is therefore a valid
 * instance of any of the three: a client that deserializes the response into the type its endpoint promises gets
 * the errors it asked about, and every other field of that type left empty — which is what a failed request
 * means. So a failure does not have to name the endpoint's return type where it is raised.
 * <p>
 * This body carries {@code errors} and nothing else. It omits {@link TableData}'s {@code data} and the
 * deprecated singular {@code error} of {@link TablesList}, which the known consumers — explorer and
 * data-lake-frontend, front and back end — do not read: they test {@code errors} for truthiness, and none of
 * them tells a {@code null} field from an absent one. Neither does Jackson.
 * <p>
 * This is only for a request that failed outright. A listing that succeeded while one catalog behind it did not
 * still answers 200 with a {@link TablesList} whose {@code errors} describe that catalog, which is the partial
 * result Data Connect asks for, and which {@code TrinoCatalog} builds rather than this record.
 *
 * @param errors what went wrong. Always at least one; a failure with nothing to say about it would tell a caller
 * nothing that the status has not already.
 */
public record DataConnectErrorResponse(@JsonProperty("errors") List<TableError> errors) {

    public static DataConnectErrorResponse of(TableError error) {
        return new DataConnectErrorResponse(List.of(error));
    }
}
