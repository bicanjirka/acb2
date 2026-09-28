package cz.cvut.fit.acb.fixtures;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Stream;

/** A seeded input of a chosen size, generated in memory to look like one kind of real data. */
public record GeneratedInput(String name, byte[] bytes) {

    private static final long SEED = 20260928L;

    public static Stream<GeneratedInput> all(int size) {
        return Stream.of(text(size), binary(size), runs(size), random(size));
    }

    /** Words from a small vocabulary with a skewed choice, so matches are plentiful but not total. */
    public static GeneratedInput text(int size) {
        Random random = new Random(SEED);
        String[] vocabulary = new String[300];
        for (int i = 0; i < vocabulary.length; i++) {
            StringBuilder word = new StringBuilder();
            for (int c = 2 + random.nextInt(8); c > 0; c--) {
                word.append((char) ('a' + random.nextInt(26)));
            }
            vocabulary[i] = word.toString();
        }
        ByteArrayOutputStream text = new ByteArrayOutputStream(size + 16);
        while (text.size() < size) {
            double skew = random.nextDouble();
            text.writeBytes(vocabulary[(int) (vocabulary.length * skew * skew)].getBytes(StandardCharsets.US_ASCII));
            text.write(' ');
        }
        return new GeneratedInput("text", trimmed(text.toByteArray(), size));
    }

    /** 16-byte records: a counter, a field from a small set, and eight noisy bytes. */
    public static GeneratedInput binary(int size) {
        Random random = new Random(SEED + 1);
        byte[] bytes = new byte[size];
        for (int at = 0; at < size; at += 16) {
            int record = at / 16;
            for (int i = 0; i < 16 && at + i < size; i++) {
                bytes[at + i] = switch (i / 4) {
                    case 0 -> (byte) (record >> (8 * (3 - i)));
                    case 1 -> (byte) (0x40 + random.nextInt(4));
                    default -> (byte) random.nextInt(256);
                };
            }
        }
        return new GeneratedInput("binary", bytes);
    }

    /** Runs of one of eight byte values, 1 to 400 long. */
    public static GeneratedInput runs(int size) {
        Random random = new Random(SEED + 2);
        byte[] bytes = new byte[size];
        int at = 0;
        while (at < size) {
            byte value = (byte) random.nextInt(8);
            for (int run = 1 + random.nextInt(400); run > 0 && at < size; run--) {
                bytes[at++] = value;
            }
        }
        return new GeneratedInput("runs", bytes);
    }

    public static GeneratedInput random(int size) {
        byte[] bytes = new byte[size];
        new Random(SEED + 3).nextBytes(bytes);
        return new GeneratedInput("random", bytes);
    }

    private static byte[] trimmed(byte[] bytes, int size) {
        return Arrays.copyOf(bytes, size);
    }

    @Override
    public String toString() {
        return this.name;
    }
}
