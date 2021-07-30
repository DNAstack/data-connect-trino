package com.dnastack.ga4gh.search.adapter.security;

import com.dnastack.ga4gh.search.adapter.presto.DateTimeUtils;
import org.checkerframework.common.value.qual.StaticallyExecutable;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DateTimeUtilsTest {

    //    public static void main(String[] args) {
    //        System.out.println("ISO8601 Timestamp with Time zone (UTC)\t"+convertToIso8601TimestampWithTimeZone("2020-05-27 12:22:27.000 UTC"));
    //        System.out.println("ISO8601 Time with Time zone (UTC)\t"+convertToIso8601TimeWithTimeZone("12:22:27.000 UTC"));
    //        System.out.println("ISO8601 Timestamp with Time zone (Los Angeles)\t"+convertToIso8601TimestampWithTimeZone("2020-05-27 12:22:27.000 America/Los_Angeles"));
    //        System.out.println("ISO8601 Time with Time zone (Los Angeles)\t"+convertToIso8601TimeWithTimeZone("12:22:27.000 America/Los_Angeles"));
    //        System.out.println();
    //        System.out.println("ISO8601 Timestamp without Time zone\t"+convertToIso8601Timestamp("2020-05-27 12:22:27.000"));
    //        System.out.println("ISO8601 Time without Time zone\t"+convertToIso8601TimeWithoutTimeZone("12:22:27.000"));
    //    }

    @Test
    public void testIso8601TimestampWithUTCTimeZone() {
        String converted = DateTimeUtils.convertToIso8601TimestampWithTimeZone("2020-05-27 12:22:27.000 UTC");
        assertEquals(converted, "2020-05-27T12:22:27.000Z");
    }

    @Test
    public void testIso8601TimeWithZone() {
        String converted = DateTimeUtils.convertToIso8601TimeWithTimeZone("12:22:27.000 UTC");
        assertEquals(converted, "12:22:27.000Z");
    }

    @Test
    public void testIso8601TimestampWithLosAngelesZone() {
        String converted = DateTimeUtils.convertToIso8601TimestampWithTimeZone("2020-05-27 12:22:27.000 America/Los_Angeles");
        assertEquals(converted, "2020-05-27T12:22:27.000-07:00");
    }

    @Test
    public void testIso8601TimewithLosAngelesZone() {
        String converted = DateTimeUtils.convertToIso8601TimeWithTimeZone("12:22:27.000 America/Los_Angeles");
        assertEquals(converted, "12:22:27.000-08:00");
    }

    @Test
    public void testIso8601TimestampWithoutZone() {
        String converted = DateTimeUtils.convertToIso8601Timestamp("2020-05-27 12:22:27.000");
        assertEquals(converted, "2020-05-27T12:22:27.000");
    }

    @Test
    public void testIso8601TimeWithoutZone() {
       String converted = DateTimeUtils.convertToIso8601TimeWithoutTimeZone("12:22:27.000");
       assertEquals(converted, "12:22:27.000");
    }
}
