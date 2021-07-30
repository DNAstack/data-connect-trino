package com.dnastack.ga4gh.search.adapter.shared;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QueryManager {

    private static final long QUERY_TIMEOUT = 5000;
    private long queryStartTime;

    // Backoff Part1: Constant Sleep
    private static final int CONST_SLEEP_TIME = 100;
    private static final int MAX_STATIC_SLEEPS = 5;
    private int constSleepCount = 0;

    // Backoff Part2: Exponential Sleep
    private static final double EXP_RATE = 1.5;
    private static final long EXP_SLEEP_OFFSET = 100;
    private static final long EXP_SLEEP_BASE = 100;
    private int expSleepCount = 0;
    private long expSleep = 0;

    // Backoff Part3: Constant Sleep
    private static final long MAX_SLEEP_TIME = 1000;

    public QueryManager() {
        queryStartTime = System.currentTimeMillis();
    }

    public void backoff() {

        long sleepTime;

        // Backoff Part1: Constant Sleep
        if (constSleepCount < MAX_STATIC_SLEEPS) {
            constSleepCount++;
            sleepTime = CONST_SLEEP_TIME;
        }
        // Backoff Part2: Exponential Sleep = OFFSET + (BASE * RATE^COUNT)
        else if (expSleep < MAX_SLEEP_TIME) {
            expSleep = (long) (EXP_SLEEP_OFFSET + (EXP_SLEEP_BASE * Math.pow(EXP_RATE, expSleepCount)));
            expSleepCount++;
            expSleep = Math.min(expSleep, MAX_SLEEP_TIME);
            sleepTime = expSleep;
        }
        // Backoff Part3: Constant Sleep
        else {
            sleepTime = MAX_SLEEP_TIME;
        }

        try {
            log.debug("Backing-off for {} ms", sleepTime);
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Request aborted due to interrupt");
        }
    }

    public boolean hasQueryTimedOut() {
        return (System.currentTimeMillis() - queryStartTime) >= QUERY_TIMEOUT;
    }
}
