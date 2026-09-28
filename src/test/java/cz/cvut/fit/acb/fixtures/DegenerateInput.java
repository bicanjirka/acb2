package cz.cvut.fit.acb.fixtures;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** An input built to hit an edge of the search or the segment end, generated in memory from a fixed seed. */
public record DegenerateInput(String name, byte[] bytes) {

    private static final String LONG_RUN = "longRunOfOneByte";
    private static final int RUN_LENGTH = 50_000;
    private static final long SEED = 20260928L;

    public static Stream<DegenerateInput> all() {
        return Stream.of(longRunOfOneByte(), allByteValues(), alternatingPair(), endingInLongMatch());
    }

    public static DegenerateInput longRunOfOneByte() {
        byte[] run = new byte[RUN_LENGTH];
        Arrays.fill(run, (byte) 'a');
        return new DegenerateInput(LONG_RUN, run);
    }

    /** Every byte value twice, in ascending and then descending order, so signed and unsigned order differ. */
    public static DegenerateInput allByteValues() {
        byte[] bytes = new byte[512];
        for (int i = 0; i < 256; i++) {
            bytes[i] = (byte) i;
            bytes[511 - i] = (byte) i;
        }
        return new DegenerateInput("allByteValues", bytes);
    }

    public static DegenerateInput alternatingPair() {
        byte[] bytes = new byte[2_000];
        IntStream.range(0, bytes.length).forEach(i -> bytes[i] = (byte) (i % 2 == 0 ? 'a' : 'b'));
        return new DegenerateInput("alternatingPair", bytes);
    }

    /** Random bytes followed by a copy of an early stretch of them, so the last match reaches the segment end. */
    public static DegenerateInput endingInLongMatch() {
        byte[] head = new byte[400];
        new Random(SEED).nextBytes(head);
        byte[] bytes = Arrays.copyOf(head, head.length + 120);
        System.arraycopy(head, 40, bytes, head.length, 120);
        return new DegenerateInput("endingInLongMatch", bytes);
    }

    public boolean isLongRun() {
        return this.name.equals(LONG_RUN);
    }

    @Override
    public String toString() {
        return this.name;
    }
}
