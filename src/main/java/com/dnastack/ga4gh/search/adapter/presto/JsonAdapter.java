package com.dnastack.ga4gh.search.adapter.presto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

@Slf4j
public class JsonAdapter {
    static final List<String> intAliases = List.of("int");
    static final List<String> numberAliases = List.of("number","float","double","real");
    static final List<String> booleanAliases = List.of("bool");

    static final ObjectMapper jsonStringMapper = new ObjectMapper();
    static{
        jsonStringMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    }

    static boolean isArray(String prestoType) {
        return prestoType.contains("[]") || prestoType.contains("array");
    }

    static PrestoDataTransformer getPrestoDataTransformer(String prestoType) {
        String lcPrestoType = prestoType.toLowerCase();

        if (lcPrestoType.startsWith("timestamp")) {
            if (lcPrestoType.endsWith("with time zone")) {
                return DateTimeUtils::convertToIso8601TimestampWithTimeZone;
            } else {
                return DateTimeUtils::convertToIso8601Timestamp;
            }
        } else if (lcPrestoType.equals("time with time zone")) {
            return DateTimeUtils::convertToIso8601TimeWithTimeZone;
        } else if (lcPrestoType.equals("time")) {
            return DateTimeUtils::convertToIso8601TimeWithoutTimeZone;
        } else if (lcPrestoType.equals("json")) {
            return JsonAdapter::convertFromJsonStringToObject;
        }

        return null;
    }

    static Object convertFromJsonStringToObject(String content) {
        try {
            return jsonStringMapper.readValue(content, Object.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String toFormat(String prestoType) {
        String lcPrestoType = prestoType.toLowerCase();
        if (lcPrestoType.startsWith("timestamp")) {
            return "date-time";
        } else if (lcPrestoType.startsWith("time")) {
            return "time";
        } else if (lcPrestoType.startsWith("date")) {
            return "date";
        }
        return prestoType;
    }

    static String toJsonType(String prestoType) {
        if (prestoType == null) {
            return "NULL";
        }

        String type = prestoType.toLowerCase();
        if (type.equals("null")) { //todo: improve null logic
            return "NULL";
        }
        if (intAliases.stream().anyMatch(type::contains)) {
            return "int";
        }
        if (numberAliases.stream().anyMatch(type::contains)) {
            return "number";
        }
        if (booleanAliases.stream().anyMatch(type::contains)) {
            return "boolean";
        }

        return "string";
    }
}
