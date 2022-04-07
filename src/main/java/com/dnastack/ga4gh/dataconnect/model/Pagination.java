package com.dnastack.ga4gh.dataconnect.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class Pagination {

    @JsonIgnore
    private String queryJobId;

    @JsonIgnore
    private String originalTraceId;

    @JsonProperty("next_page_url")
    private URI nextPageUrl;

    @JsonIgnore
    private URI trinoNextPageUrl;

    public URI getNextPageUrl() {
        if (queryJobId != null && nextPageUrl != null && originalTraceId != null) {
            return UriComponentsBuilder.fromUri(nextPageUrl)
                .queryParam("queryJobId", queryJobId)
                .queryParam("originalTraceId", originalTraceId)
                .build()
                .toUri();
        }
        return nextPageUrl;
    }

}
