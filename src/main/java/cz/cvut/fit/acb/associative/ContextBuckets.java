package cz.cvut.fit.acb.associative;

import cz.cvut.fit.acb.mixing.Logistic;

/** The coarse scales the mixed models learn in: a few buckets for a depth, a count, an agreement, a distance. */
final class ContextBuckets {

    /** Buckets of {@link #depth}. */
    static final int DEPTHS = 28;
    /** Buckets of {@link #count}. */
    static final int COUNTS = 6;
    /** Buckets of {@link #agreement}. */
    static final int AGREEMENTS = 16;
    /** Buckets of {@link #distance}. */
    static final int DISTANCES = 24;

    private ContextBuckets() {
    }

    /** Every depth to 15 alone, then one bucket per doubling. */
    static int depth(int depth) {
        return depth < 16 ? depth : Math.min(DEPTHS - 1, 12 + log2(depth));
    }

    /** 1, 2, 3 to 4, 5 to 8, 9 to 16, and more, for a count of at least 1. */
    static int count(int count) {
        return Math.min(COUNTS - 1, Integer.SIZE - Integer.numberOfLeadingZeros(count - 1));
    }

    /** Whole bytes of an agreement in bits: each to 7 alone, then one bucket per doubling. */
    static int agreement(int bits) {
        int bytes = bits >> 3;
        return bytes < 8 ? bytes : Math.min(AGREEMENTS - 1, 5 + log2(bytes));
    }

    /** One bucket per doubling of a distance in bytes. */
    static int distance(int distance) {
        return Math.min(DISTANCES - 1, log2(distance));
    }

    /** The 12-bit probability that {@code part} of {@code whole} stands for, kept away from 0 and 1. */
    static int share(long part, long whole) {
        return (int) Math.max(1, Math.min(Logistic.ONE - 1, (part << Logistic.BITS) / Math.max(1, whole)));
    }

    /** How many bits a hashed table needs for a segment of {@code length} bytes: enough, and no more than {@code max}. */
    static int tableBits(int length, int perByteBits, int max) {
        return Math.max(10, Math.min(max, log2(Math.max(1, length)) + 1 + perByteBits));
    }

    private static int log2(int value) {
        return value < 2 ? 0 : Integer.SIZE - 1 - Integer.numberOfLeadingZeros(value);
    }
}
