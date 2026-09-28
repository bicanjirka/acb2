package cz.cvut.fit.acb.harness;

import cz.cvut.fit.acb.CompressionSettings;
import cz.cvut.fit.acb.Compressor;
import cz.cvut.fit.acb.EntropyCoding;
import cz.cvut.fit.acb.TripletCoding;
import cz.cvut.fit.acb.fixtures.GeneratedInput;
import cz.cvut.fit.acb.fixtures.SettingsCombination;
import cz.cvut.fit.acb.format.CompressedStream;
import cz.cvut.fit.acb.format.MalformedStreamException;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Times compression and decompression of seeded text-like, binary, run-heavy and random inputs for
 * each coder (length field of 7 bits, defaults otherwise), after a warm-up round trip, and exits
 * non-zero when a coder is under its {@link PerformanceBudget}. The median of the repetitions is
 * used for each input; the coder's figure is the total bytes over the total of those medians.
 *
 * <pre>
 * mvn -q package -DskipTests
 * java -cp "target/acb.jar;target/test-classes" cz.cvut.fit.acb.harness.PerformanceHarness [size=131072] [reps=3]
 * </pre>
 *
 * (Use {@code :} instead of {@code ;} outside Windows.)
 */
public final class PerformanceHarness {

    private static final int DEFAULT_SIZE = 131_072;
    private static final int DEFAULT_REPETITIONS = 3;
    private static final int LENGTH_BITS = 7;

    /** One coder's speed over all the inputs. */
    record Speed(TripletCoding coder, double compress, double decompress) {
    }

    private PerformanceHarness() {
    }

    public static void main(String[] args) {
        int size = DEFAULT_SIZE;
        int repetitions = DEFAULT_REPETITIONS;
        for (String arg : args) {
            String[] keyValue = arg.split("=", 2);
            switch (keyValue[0]) {
                case "size" -> size = Integer.parseInt(keyValue[1]);
                case "reps" -> repetitions = Integer.parseInt(keyValue[1]);
                default -> throw new IllegalArgumentException("Unknown option " + arg);
            }
        }
        boolean withinBudget = run(size, repetitions, PerformanceBudget.current(), System.out);
        System.exit(withinBudget ? 0 : 1);
    }

    static boolean run(int size, int repetitions, PerformanceBudget budget, PrintStream out) {
        List<GeneratedInput> inputs = GeneratedInput.all(size).toList();
        out.printf(Locale.ROOT, "%d inputs of %,d bytes, median of %d, length field %d bits%n%n", inputs.size(), size,
                repetitions, LENGTH_BITS);
        out.printf(Locale.ROOT, "%-10s %10s %10s %10s %10s%n", "coder", "comp MB/s", "dec MB/s", "floor comp",
                "floor dec");
        boolean withinBudget = true;
        for (TripletCoding coder : workingCoders()) {
            Speed speed = measure(coder, inputs, repetitions);
            PerformanceBudget.Floor floor = budget.floorFor(coder);
            boolean fast = speed.compress() >= floor.compress() && speed.decompress() >= floor.decompress();
            out.printf(Locale.ROOT, "%-10s %10.2f %10.2f %10.2f %10.2f%s%n",
                    coder.name().toLowerCase(Locale.ROOT), speed.compress(), speed.decompress(), floor.compress(),
                    floor.decompress(), fast ? "" : "  UNDER BUDGET");
            withinBudget &= fast;
        }
        return withinBudget;
    }

    static Speed measure(TripletCoding coder, List<GeneratedInput> inputs, int repetitions) {
        Compressor compressor = new Compressor(CompressionSettings.defaults().withTripletCoding(coder)
                .withLengthBits(LENGTH_BITS));
        long bytes = 0;
        double compressSeconds = 0;
        double decompressSeconds = 0;
        for (GeneratedInput input : inputs) {
            roundTrip(compressor, input.bytes());
            long[] compress = new long[repetitions];
            long[] decompress = new long[repetitions];
            for (int rep = 0; rep < repetitions; rep++) {
                long start = System.nanoTime();
                CompressedStream stream = compressor.compress(input.bytes());
                long middle = System.nanoTime();
                decompress(compressor, stream);
                long end = System.nanoTime();
                compress[rep] = middle - start;
                decompress[rep] = end - middle;
            }
            bytes += input.bytes().length;
            compressSeconds += median(compress) / 1e9;
            decompressSeconds += median(decompress) / 1e9;
        }
        return new Speed(coder, bytes / 1e6 / compressSeconds, bytes / 1e6 / decompressSeconds);
    }

    private static List<TripletCoding> workingCoders() {
        return Arrays.stream(TripletCoding.values())
                .filter(coder -> new SettingsCombination(coder, EntropyCoding.ADAPTIVE_ARITHMETIC).knownRoundTripDefect().isEmpty())
                .toList();
    }

    private static void roundTrip(Compressor compressor, byte[] input) {
        if (!Arrays.equals(decompress(compressor, compressor.compress(input)), input)) {
            throw new IllegalStateException("A warm-up round trip changed its input");
        }
    }

    private static byte[] decompress(Compressor compressor, CompressedStream stream) {
        try {
            return compressor.decompress(stream);
        } catch (MalformedStreamException e) {
            throw new IllegalStateException("A stream just compressed must decode", e);
        }
    }

    private static long median(long[] values) {
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }
}
