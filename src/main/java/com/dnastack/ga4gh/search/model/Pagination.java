package com.dnastack.ga4gh.search.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.web.util.UriComponentsBuilder;

@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class Pagination {
    @JsonIgnore
    private String queryJobId;

    @JsonProperty("next_page_url")
    private URI nextPageUrl;

    @JsonIgnore
    private URI prestoNextPageUrl;

    public URI getNextPageUrl() {
        if (queryJobId != null && nextPageUrl != null) {
            return UriComponentsBuilder.fromUri(nextPageUrl)
                                       .queryParam("queryJobId", queryJobId)
                                       .build()
                                       .toUri();
        }
        return nextPageUrl;
    }

}
