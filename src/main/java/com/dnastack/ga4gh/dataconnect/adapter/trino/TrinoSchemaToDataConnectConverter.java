package com.dnastack.ga4gh.dataconnect.adapter.trino;

import com.dnastack.ga4gh.dataconnect.adapter.trino.exception.UnexpectedQueryResponseException;
import com.dnastack.ga4gh.dataconnect.model.ColumnSchema;
import com.dnastack.ga4gh.dataconnect.model.DataModel;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.util.Strings;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class TrinoSchemaToDataConnectConverter {

    private TrinoSchemaToDataConnectConverter() {
    }

    private static final URI JSON_SCHEMA_DRAFT7_URI = URI.create("http://json-schema.org/draft-07/schema#");
    private static final String OBJECT_TYPE_NAME = "object";


    public static DataModel generateDataModel(JsonNode columns) {
        return DataModel.builder()
            .id(null)
            .description("Automatically generated schema")
            .schema(JSON_SCHEMA_DRAFT7_URI)
            .properties(getJsonSchemaProperties(columns))
            .build();
    }

    private static Map<String, ColumnSchema> getJsonSchemaProperties(JsonNode columns) {

        return StreamSupport.stream(columns.spliterator(), false)
            .map(column -> Map.entry(column.get("name").asText(), getColumnSchema(column.get("typeSignature"))))
            .collect(toLinkedHashMap(Map.Entry::getKey, Map.Entry::getValue));

    }

    private static ColumnSchema getColumnSchema(JsonNode trinoTypeDescription) {
        final String rawType = trinoTypeDescription.get("rawType").asText();
        final String format = JsonAdapter.toFormat(trinoTypeDescription.get("rawType").asText());
        if (rawType.equalsIgnoreCase("array")) {
            ColumnSchema columnSchema = getColumnSchema(trinoTypeDescription.get("arguments").get(0).get("value"));
            return ColumnSchema.builder()
                .type("array")
                .rawType(rawType)
                .comment("array[" + columnSchema.getType() + "]")
                .items(columnSchema)
                .build();
        } else {
            if (rawType.equalsIgnoreCase("row")) {
                JsonNode args = trinoTypeDescription.get("arguments");

                Map<String, ColumnSchema> m = StreamSupport.stream(args.spliterator(), false)
                    .collect(
                        Collectors.toMap(rowArg -> rowArg.get("value").get("fieldName").get("name").asText(),
                            rowArg -> getColumnSchema(rowArg.get("value").get("typeSignature")),
                            (k, v) -> {throw new UnexpectedQueryResponseException("rows must have unique key names. Duplicate key " + k + ", value=" + v);},
                            LinkedHashMap::new)); //maintain key order to generate better comment.


                return ColumnSchema.builder()
                    .type(OBJECT_TYPE_NAME)
                    .rawType(rawType)
                    .comment(String.format("row(%s)", Strings.join(
                        m.values().stream()
                            .map(ColumnSchema::getType)
                            .toList(), ',')))
                    .properties(m)
                    .build();
            } else if (rawType.equalsIgnoreCase("map")) {

                ColumnSchema keySchema = getColumnSchema(trinoTypeDescription.get("arguments").get(0).get("value"));
                ColumnSchema valueSchema = getColumnSchema(trinoTypeDescription.get("arguments").get(1).get("value"));

                return ColumnSchema.builder()
                    .type(OBJECT_TYPE_NAME)
                    .rawType(rawType)
                    .comment(String.format("map(%s, %s)", keySchema.getType(), valueSchema.getType()))
                    .properties(Map.of("key", keySchema, "value", valueSchema))
                    .build();

            } else if (rawType.equalsIgnoreCase("json")) {
                return ColumnSchema.builder()
                    .type(OBJECT_TYPE_NAME)
                    .rawType(rawType)
                    .comment("json")
                    .build();
            } else {
                //must be a primitive.
                String type = JsonAdapter.toJsonType(rawType);
                return ColumnSchema.builder()
                    .type(type)
                    .rawType(rawType)
                    .comment(rawType)
                    .format(format)
                    .build();
            }
        }

    }


    private static <T, K, U> Collector<T, ?, Map<K, U>> toLinkedHashMap(
        Function<? super T, ? extends K> keyMapper,
        Function<? super T, ? extends U> valueMapper
    ) {
        return Collectors.toMap(keyMapper,
            valueMapper,
            (k, v) -> {throw new UnexpectedQueryResponseException("Duplicate key " + k);},
            LinkedHashMap::new);
    }

}
