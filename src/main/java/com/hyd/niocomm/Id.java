package com.hyd.niocomm;

/**
 * Unique id is composed of:
 * current time stamp - 47 bits (millisecond precision w/a custom epoch gives as 69 years)
 * sequence number - 17 bits - rolls over every 65536 with protection to avoid rollover in the same ms
 *
 * https://stackoverflow.com/a/42797708/395196
 **/
public class Id {

    private static final long twepoch = 1288834974657L;

    private static final long sequenceBits = 17;

    private static final long sequenceMax = 65536;

    private static volatile long lastTimestamp = -1L;

    private static volatile long sequence = 0L;

    public static synchronized Long next() {
        long timestamp = System.currentTimeMillis();
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) % sequenceMax;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = timestamp;
        Long id = ((timestamp - twepoch) << sequenceBits) | sequence;
        return id;
    }

    private static long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}