package com.dnastack.ga4gh.search.client.tablesregistry.model;

import com.dnastack.ga4gh.search.model.DataModel;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TableCollectionEntry {
    private String registryId;
    private String name;
    private String description;
    private DataModel tableSchema;
}
