package com.dnastack.ga4gh.search.client.tablesregistry.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// Response object received from the tables-registry Query REST api
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ListTableRegistryEntry {
    private List<TableCollectionEntry> tableCollections;
}
