package com.dnastack.ga4gh.search.adapter.trino;

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

    static boolean isArray(String trinoType) {
        return trinoType.contains("[]") || trinoType.contains("array");
    }

    static TrinoDataTransformer getTrinoDataTransformer(String trinoType) {
        String lcTrinoType = trinoType.toLowerCase();

        if (lcTrinoType.startsWith("timestamp")) {
            if (lcTrinoType.endsWith("with time zone")) {
                return DateTimeUtils::convertToIso8601TimestampWithTimeZone;
            } else {
                return DateTimeUtils::convertToIso8601Timestamp;
            }
        } else if (lcTrinoType.equals("time with time zone")) {
            return DateTimeUtils::convertToIso8601TimeWithTimeZone;
        } else if (lcTrinoType.equals("time")) {
            return DateTimeUtils::convertToIso8601TimeWithoutTimeZone;
        } else if (lcTrinoType.equals("json")) {
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

    static String toFormat(String trinoType) {
        String lcTrinoType = trinoType.toLowerCase();
        if (lcTrinoType.startsWith("timestamp")) {
            return "date-time";
        } else if (lcTrinoType.startsWith("time")) {
            return "time";
        } else if (lcTrinoType.startsWith("date")) {
            return "date";
        }
        return trinoType;
    }

    static String toJsonType(String trinoType) {
        if (trinoType == null) {
            return "NULL";
        }

        String type = trinoType.toLowerCase();
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
