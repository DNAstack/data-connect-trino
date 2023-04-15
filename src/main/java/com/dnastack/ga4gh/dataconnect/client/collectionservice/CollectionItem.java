package com.dnastack.ga4gh.dataconnect.client.collectionservice;

import com.dnastack.ga4gh.dataconnect.model.DataModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CollectionItem {
    private String collectionId;
    private Instant cachedAt;
    private String id;
    private String type;
    private String dataSourceName;
    private String dataSourceType;
    private String name;
    private String displayName;
    private String description;
    private DataModel jsonSchema;
    private Instant createdTime;
    private Instant updatedTime;
    private Long size;
    private String sizeUnit;
    private String dataSourceUrl;
    private Instant itemUpdatedAt;
}
