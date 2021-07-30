package com.dnastack.ga4gh.search.adapter.presto;

import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;
import org.joda.time.format.DateTimePrinter;
import org.joda.time.format.ISODateTimeFormat;

// Based on https://github.com/prestosql/presto/blob/62cf1510d7cec2cd762711b6b3358d7e3259ac65/presto-main/src/main/java/io/prestosql/util/DateTimeUtils.java

@Slf4j
public class DateTimeUtils {
    private static final DateTimeFormatter TIMESTAMP_WITH_TIME_ZONE_FORMATTER;
    private static final DateTimeFormatter TIMESTAMP_WITHOUT_TIME_ZONE_FORMATTER;

    static {
        DateTimeParser[] timestampWithoutTimeZoneParser = {
                DateTimeFormat.forPattern("yyyyyy-M-d").getParser(),
                DateTimeFormat.forPattern("yyyyyy-M-d H:m").getParser(),
                DateTimeFormat.forPattern("yyyyyy-M-d H:m:s").getParser(),
                DateTimeFormat.forPattern("yyyyyy-M-d H:m:s.SSS").getParser()};

        DateTimeParser[] timestampWithTimeZoneParser = {
                DateTimeFormat.forPattern("yyyyyy-M-dZ").getParser(),
                DateTimeFormat.forPattern("yyyyyy-M-d Z").getParser(),
                DateTimeFormat.forPattern("yyyyyy-M-d H:mZ").getParser(),
                DateTimeFormat.forPattern("yyyyyy-M-d H:m Z").getParser(),
                DateTimeFormat.forPattern("yyyyyy-M-d H:m:sZ").getParser(),
                DateTimeFormat.forPattern("yyyyyy-M-d H:m:s Z").getParser(),
                DateTimeFormat.forPattern("yyyyyy-M-d H:m:s.SSSZ").getParser(),
                DateTimeFormat.forPattern("yyyyyy-M-d H:m:s.SSS Z").getParser(),
                DateTimeFormat.forPattern("yyyyyy-M-dZZZ").getParser(),
                DateTimeFormat.forPattern("yyyyyy-M-d ZZZ").getParser(),
                DateTimeFormat.forPattern("yyyyyy-M-d H:mZZZ").getParser(),
                DateTimeFormat.forPattern("yyyyyy-M-d H:m ZZZ").getParser(),
                DateTimeFormat.forPattern("yyyyyy-M-d H:m:sZZZ").getParser(),
                DateTimeFormat.forPattern("yyyyyy-M-d H:m:s ZZZ").getParser(),
                DateTimeFormat.forPattern("yyyyyy-M-d H:m:s.SSSZZZ").getParser(),
                DateTimeFormat.forPattern("yyyyyy-M-d H:m:s.SSS ZZZ").getParser()};

        DateTimePrinter timestampWithTimeZonePrinter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS ZZZ").getPrinter();
        TIMESTAMP_WITH_TIME_ZONE_FORMATTER = new DateTimeFormatterBuilder()
                .append(timestampWithTimeZonePrinter, timestampWithTimeZoneParser)
                .toFormatter()
                .withOffsetParsed();

        DateTimePrinter timestampWithoutTimeZonePrinter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS").getPrinter();
        TIMESTAMP_WITHOUT_TIME_ZONE_FORMATTER = new DateTimeFormatterBuilder()
                .append(timestampWithoutTimeZonePrinter, timestampWithoutTimeZoneParser)
                .toFormatter();

    }

    /**
     * Parse a string in Presto's TIMESTAMP WITH TIME ZONE format,
     * and return it in ISO 8601 format.
     * <p>
     * For example: {@code "2020-05-27 12:22:27.000 UTC"} is parsed to
     * {@code 2020-05-27T12:22:27.000Z}.
     * </p>
     * @return iso8601 form of timestamp with timezone.
     */
    public static String convertToIso8601TimestampWithTimeZone(String timestampWithTimeZone) {
        if (timestampWithTimeZone.equalsIgnoreCase("null")) {
            return null;
        }
        String ts = TIMESTAMP_WITH_TIME_ZONE_FORMATTER
                .withOffsetParsed()
                .parseDateTime(timestampWithTimeZone)
                .toDateTimeISO().toString();
        return ts;
    }

    /**
     * Parse a string in Presto's TIMESTAMP format and return it in ISO 8601 format.
     * <p>
     * For example: {@code "2020-05-27 12:22:27.000"} is parsed to
     * {@code 2020-05-27T12:22:27.000}.
     * </p>
     * @return iso8601 form of timestamp with timezone.
     */
    public static String convertToIso8601Timestamp(String timestamp) {
        if (timestamp.equalsIgnoreCase("null")) {
            return null;
        }
        String ts = TIMESTAMP_WITHOUT_TIME_ZONE_FORMATTER
                .parseDateTime(timestamp)
                .toDateTimeISO()
                .toLocalDateTime().toString();
        return ts;
    }


    private static final DateTimeFormatter TIME_FORMATTER;
    private static final DateTimeFormatter TIME_WITH_TIME_ZONE_FORMATTER;

    static {
        DateTimeParser[] timeWithoutTimeZoneParser = {
                DateTimeFormat.forPattern("H:m").getParser(),
                DateTimeFormat.forPattern("H:m:s").getParser(),
                DateTimeFormat.forPattern("H:m:s.SSS").getParser()};
        DateTimePrinter timeWithoutTimeZonePrinter = DateTimeFormat.forPattern("HH:mm:ss.SSS").getPrinter();
        TIME_FORMATTER = new DateTimeFormatterBuilder().append(timeWithoutTimeZonePrinter, timeWithoutTimeZoneParser).toFormatter().withZoneUTC();

        DateTimeParser[] timeWithTimeZoneParser = {
                DateTimeFormat.forPattern("H:mZ").getParser(),
                DateTimeFormat.forPattern("H:m Z").getParser(),
                DateTimeFormat.forPattern("H:m:sZ").getParser(),
                DateTimeFormat.forPattern("H:m:s Z").getParser(),
                DateTimeFormat.forPattern("H:m:s.SSSZ").getParser(),
                DateTimeFormat.forPattern("H:m:s.SSS Z").getParser(),
                DateTimeFormat.forPattern("H:mZZZ").getParser(),
                DateTimeFormat.forPattern("H:m ZZZ").getParser(),
                DateTimeFormat.forPattern("H:m:sZZZ").getParser(),
                DateTimeFormat.forPattern("H:m:s ZZZ").getParser(),
                DateTimeFormat.forPattern("H:m:s.SSSZZZ").getParser(),
                DateTimeFormat.forPattern("H:m:s.SSS ZZZ").getParser()};
        DateTimePrinter timeWithTimeZonePrinter = DateTimeFormat.forPattern("HH:mm:ss.SSS ZZZ").getPrinter();
        TIME_WITH_TIME_ZONE_FORMATTER = new DateTimeFormatterBuilder().append(timeWithTimeZonePrinter, timeWithTimeZoneParser).toFormatter().withOffsetParsed();
    }

    /**
     * Parse a string containing a zone as a value of TIME WITH TIME ZONE type.
     * <p>
     * For example: {@code "01:23:00 +01:23"} is parsed to TIME WITH TIME ZONE
     * {@code 01:23:00+01:23} and {@code "01:23:00"} is rejected.
     *
     * @return stack representation of TIME WITH TIME ZONE type
     */
    public static String convertToIso8601TimeWithTimeZone(String timeWithTimeZone) {
        if (timeWithTimeZone.equalsIgnoreCase("null")) {
            return null;
        }

        DateTime dt = TIME_WITH_TIME_ZONE_FORMATTER
                .parseDateTime(timeWithTimeZone)
                .toDateTimeISO();

        return ISODateTimeFormat.time().print(dt);

    }

    /**
     * Parse a string (without a zone) as a value of TIME type.
     * <p>
     * For example: {@code "01:23:00"} is parsed to TIME {@code 01:23:00}
     * and {@code "01:23:00 +01:23"} is rejected.
     *
     * @return stack representation of TIME type
     */
    public static String convertToIso8601TimeWithoutTimeZone(String value) {
        if (value.equalsIgnoreCase("null")) {
            return null;
        }
        return TIME_FORMATTER.parseDateTime(value).toDateTimeISO().toLocalTime().toString();
    }

//
//    public static void main(String[] args) {
//        System.out.println("ISO8601 Timestamp with Time zone (UTC)\t"+convertToIso8601TimestampWithTimeZone("2020-05-27 12:22:27.000 UTC"));
//        System.out.println("ISO8601 Time with Time zone (UTC)\t"+convertToIso8601TimeWithTimeZone("12:22:27.000 UTC"));
//        System.out.println("ISO8601 Timestamp with Time zone (Los Angeles)\t"+convertToIso8601TimestampWithTimeZone("2020-05-27 12:22:27.000 America/Los_Angeles"));
//        System.out.println("ISO8601 Time with Time zone (Los Angeles)\t"+convertToIso8601TimeWithTimeZone("12:22:27.000 America/Los_Angeles"));
//        System.out.println();
//        System.out.println("ISO8601 Timestamp without Time zone\t"+convertToIso8601Timestamp("2020-05-27 12:22:27.000"));
//        System.out.println("ISO8601 Time without Time zone\t"+convertToIso8601TimeWithoutTimeZone("12:22:27.000"));
//    }
}
