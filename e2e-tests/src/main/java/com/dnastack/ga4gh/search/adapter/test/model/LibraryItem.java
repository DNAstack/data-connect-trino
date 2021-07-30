package com.dnastack.ga4gh.search.adapter.test.model;

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
public class LibraryItem {
    private String id;
    private String type;
    private String dataSourceName;
    private String dataSourceType;
    private String sourceKey;
    private String name;
    private String description;
    private String preferredName;
    private List<String> aliases;
    private Map<String, String> preferredColumnNames;
    private String jsonSchema;
    // private List<Checksum> checksums; // Not used here
    // private List<DRSContentObject> bundleContents; // Not used here
    private Instant createdTime;
    private Instant updatedTime;
    // private String mimeType; // Not used here
    private Long size;
    private String sizeUnit;
    private String version;
    private String dataSourceUrl;
    private Instant itemUpdatedTime;

    // Not used here
//    @Data
//    @AllArgsConstructor
//    @NoArgsConstructor
//    @Builder
//    public static class DRSChecksum {
//        private String checksum;
//        private String type;
//    }
//
//    @Data
//    @AllArgsConstructor
//    @NoArgsConstructor
//    @Builder
//    public static class DRSContentObject {
//        private String name;
//        private String id;
//        private List<String> drsUri;
//        private List<DRSContentObject> contents;
//    }
}
