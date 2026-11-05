package com.dnastack.ga4gh.dataconnect.adapter.security;

import com.dnastack.ga4gh.dataconnect.adapter.trino.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
    public void testIso8601TimestampWithoutZone_whenItFallsInTheDefaultZonesDaylightSavingGap() {
        // 2026-03-08 02:00 does not exist in America/Toronto: clocks jump from 02:00 to 03:00. Joda's default zone is
        // process-wide, so this test must not run in parallel with others in the same JVM; it restores the zone after.
        DateTimeZone original = DateTimeZone.getDefault();
        DateTimeZone.setDefault(DateTimeZone.forID("America/Toronto"));
        try {
            assertEquals("2026-03-08T02:00:00.000", DateTimeUtils.convertToIso8601Timestamp("2026-03-08 02:00:00.000"));
            assertEquals("2026-03-08T02:30:15.000", DateTimeUtils.convertToIso8601Timestamp("2026-03-08 2:30:15"));
            assertEquals("2026-11-01T01:30:00.000", DateTimeUtils.convertToIso8601Timestamp("2026-11-01 01:30:00.000"));
        } finally {
            DateTimeZone.setDefault(original);
        }
    }

    @Test
    public void testIso8601TimestampWithoutZone_whenOnlyADateIsGiven() {
        assertEquals("2020-05-27T00:00:00.000", DateTimeUtils.convertToIso8601Timestamp("2020-05-27"));
    }

    @Test
    public void testIso8601TimestampWithoutZone_whenNull() {
        assertNull(DateTimeUtils.convertToIso8601Timestamp("null"));
    }

    @Test
    public void testIso8601TimeWithoutZone() {
       String converted = DateTimeUtils.convertToIso8601TimeWithoutTimeZone("12:22:27.000");
       assertEquals(converted, "12:22:27.000");
    }
}
