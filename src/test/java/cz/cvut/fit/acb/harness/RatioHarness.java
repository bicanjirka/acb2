package cz.cvut.fit.acb.harness;

import cz.cvut.fit.acb.CompressionResult;
import cz.cvut.fit.acb.CompressionSettings;
import cz.cvut.fit.acb.CompressionStats;
import cz.cvut.fit.acb.Compressor;
import cz.cvut.fit.acb.EntropyCoding;
import cz.cvut.fit.acb.TripletCoding;
import cz.cvut.fit.acb.format.ContainerFormat;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.triplets.TripletFieldKind;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Compresses every file of a corpus directory with each settings combination asked for, checks that
 * each decompresses to its input, and prints size, bits per character, speed and what each kind of
 * triplet field cost. Files are compressed one by one and the sizes added up, as the reference
 * compressors are run.
 *
 * <pre>
 * mvn -q package -DskipTests
 * java -cp "target/acb.jar;target/test-classes" cz.cvut.fit.acb.harness.RatioHarness DIR [key=a,b ...]
 * </pre>
 *
 * (Use {@code :} instead of {@code ;} outside Windows.) Keys: {@code coders} (default all
 * coders), {@code d}, {@code l} and {@code depth} (default what the coder starts from, except that the
 * layout coders are run at both 4 and 7 bits of length), {@code entropy} ({@code arith}, {@code bits},
 * {@code context}; default arith, and a coder that does not use an entropy coding is left out of it).
 */
public final class RatioHarness {

    /** One settings combination's totals over the whole corpus. */
    record Row(String label, long inputBytes, long outputBytes, CompressionStats stats, long compressNanos,
               long decompressNanos) {

        double bitsPerCharacter() {
            return this.inputBytes == 0 ? 0 : 8.0 * this.outputBytes / this.inputBytes;
        }

        double compressMegabytesPerSecond() {
            return megabytesPerSecond(this.inputBytes, this.compressNanos);
        }

        double decompressMegabytesPerSecond() {
            return megabytesPerSecond(this.inputBytes, this.decompressNanos);
        }
    }

    private RatioHarness() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("usage: RatioHarness DIR [coders=a,b] [d=6,10] [l=4,7] [depth=10] [entropy=arith,bits,context]");
            System.exit(2);
        }
        run(Path.of(args[0]), Arrays.copyOfRange(args, 1, args.length), System.out);
    }

    static List<Row> run(Path corpus, String[] options, PrintStream out) throws IOException {
        List<byte[]> files = read(corpus);
        long total = files.stream().mapToLong(file -> file.length).sum();
        List<CompressionSettings> combinations = settingsFor(options).toList();
        if (!files.isEmpty() && !combinations.isEmpty()) {
            measure(combinations.getFirst(), List.of(smallest(files)));
        }
        out.printf(Locale.ROOT, "%s: %d files, %,d bytes%n%n", corpus, files.size(), total);
        out.printf(Locale.ROOT, "%-36s %11s %6s %9s %8s %10s %10s %10s %10s%n", "settings", "bytes", "bpc",
                "comp MB/s", "dec MB/s", "flag B", "distance B", "length B", "literal B");
        List<Row> rows = combinations.stream().map(settings -> {
            Row row = measure(settings, files);
            out.println(format(row));
            return row;
        }).toList();
        if (total == ReferenceResults.CALGARY_BYTES) {
            out.printf(Locale.ROOT, "%nReference results on the same corpus:%n");
            ReferenceResults.calgary().forEach(result -> out.printf(Locale.ROOT, "%-30s %,11d %6.2f%n",
                    result.name(), result.bytes(), 8.0 * result.bytes() / total));
        }
        return rows;
    }

    /** Compresses and decompresses every file, throwing if any does not come back the same. */
    static Row measure(CompressionSettings settings, List<byte[]> files) {
        Compressor compressor = new Compressor(settings);
        CompressionStats stats = CompressionStats.none();
        long outputBytes = 0;
        long compressNanos = 0;
        long decompressNanos = 0;
        long inputBytes = 0;
        for (byte[] file : files) {
            long start = System.nanoTime();
            CompressionResult result = compressor.compressWithStats(file);
            long middle = System.nanoTime();
            byte[] decompressed = decompress(compressor, result);
            long end = System.nanoTime();
            if (!Arrays.equals(decompressed, file)) {
                throw new IllegalStateException(describe(settings) + " does not round-trip a " + file.length
                        + " byte file");
            }
            stats = stats.plus(result.stats());
            outputBytes += ContainerFormat.encode(result.stream()).length;
            compressNanos += middle - start;
            decompressNanos += end - middle;
            inputBytes += file.length;
        }
        return new Row(describe(settings), inputBytes, outputBytes, stats, compressNanos, decompressNanos);
    }

    static String describe(CompressionSettings settings) {
        return String.format(Locale.ROOT, "%s d=%d l=%d c=%d %s",
                settings.tripletCoding().name().toLowerCase(Locale.ROOT),
                settings.distanceBits(), settings.lengthBits(), settings.contextDepth(),
                entropyName(settings.entropyCoding()));
    }

    private static String entropyName(EntropyCoding entropy) {
        return switch (entropy) {
            case ADAPTIVE_ARITHMETIC -> "arith";
            case BIT_ARRAY -> "bits";
            case CONTEXT_ARITHMETIC -> "context";
        };
    }

    private static EntropyCoding entropyOf(String name) {
        return Arrays.stream(EntropyCoding.values()).filter(entropy -> entropyName(entropy).equals(name))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown entropy " + name));
    }

    static double megabytesPerSecond(long bytes, long nanos) {
        return nanos == 0 ? 0 : bytes / 1_000_000.0 / (nanos / 1_000_000_000.0);
    }

    private static byte[] decompress(Compressor compressor, CompressionResult result) {
        try {
            return compressor.decompress(result.stream());
        } catch (MalformedStreamException e) {
            throw new IllegalStateException("A stream just compressed must decode", e);
        }
    }

    private static String format(Row row) {
        return String.format(Locale.ROOT, "%-36s %,11d %6.2f %9.2f %8.2f %,10d %,10d %,10d %,10d", row.label(),
                row.outputBytes(), row.bitsPerCharacter(), row.compressMegabytesPerSecond(),
                row.decompressMegabytesPerSecond(), bytesOf(row, TripletFieldKind.FLAG),
                bytesOf(row, TripletFieldKind.DISTANCE), bytesOf(row, TripletFieldKind.LENGTH),
                bytesOf(row, TripletFieldKind.LITERAL));
    }

    private static long bytesOf(Row row, TripletFieldKind kind) {
        return row.stats().cost(kind).bits() / Byte.SIZE;
    }

    private static byte[] smallest(List<byte[]> files) {
        return files.stream().min((a, b) -> Integer.compare(a.length, b.length)).orElseThrow();
    }

    private static Stream<CompressionSettings> settingsFor(String[] options) {
        List<TripletCoding> coders = Arrays.asList(TripletCoding.values());
        int[] distances = null;
        int[] lengths = null;
        int[] depths = null;
        List<EntropyCoding> entropies = List.of(EntropyCoding.ADAPTIVE_ARITHMETIC);
        for (String option : options) {
            String[] keyValue = option.split("=", 2);
            if (keyValue.length != 2) {
                throw new IllegalArgumentException("Expected key=value, got " + option);
            }
            String[] values = keyValue[1].split(",");
            switch (keyValue[0]) {
                case "coders" -> coders = Arrays.stream(values)
                        .map(value -> TripletCoding.valueOf(value.toUpperCase(Locale.ROOT))).toList();
                case "d" -> distances = Arrays.stream(values).mapToInt(Integer::parseInt).toArray();
                case "l" -> lengths = Arrays.stream(values).mapToInt(Integer::parseInt).toArray();
                case "depth" -> depths = Arrays.stream(values).mapToInt(Integer::parseInt).toArray();
                case "entropy" -> entropies = Arrays.stream(values).map(RatioHarness::entropyOf).toList();
                default -> throw new IllegalArgumentException("Unknown key " + keyValue[0]);
            }
        }
        List<CompressionSettings> combinations = new ArrayList<>();
        for (EntropyCoding entropy : entropies) {
            for (TripletCoding coder : coders) {
                if (!coder.entropyCodings().contains(entropy)) {
                    continue;
                }
                CompressionSettings start = CompressionSettings.defaultsFor(coder);
                for (int distance : distances != null ? distances : new int[]{start.distanceBits()}) {
                    for (int length : lengths != null ? lengths : defaultLengths(coder, start)) {
                        for (int depth : depths != null ? depths : new int[]{start.contextDepth()}) {
                            combinations.add(start.withEntropyCoding(entropy).withDistanceBits(distance)
                                    .withLengthBits(length).withContextDepth(depth));
                        }
                    }
                }
            }
        }
        return combinations.stream();
    }

    /** The layout coders are compared at a short and a long length field, as the thesis does. */
    private static int[] defaultLengths(TripletCoding coder, CompressionSettings start) {
        return coder.layoutCoder().isPresent() ? new int[]{4, 7} : new int[]{start.lengthBits()};
    }

    private static List<byte[]> read(Path corpus) throws IOException {
        try (Stream<Path> paths = Files.list(corpus)) {
            return paths.filter(Files::isRegularFile).sorted().map(RatioHarness::readAll).toList();
        }
    }

    private static byte[] readAll(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
