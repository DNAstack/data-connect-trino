package com.dnastack.ga4gh.dataconnect.client.tablesregistry.model;

import com.dnastack.ga4gh.dataconnect.model.DataModel;
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
