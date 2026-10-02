package cz.cvut.fit.acb.harness;

import cz.cvut.fit.acb.Compressor;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Times one coder of two builds against each other in one JVM: this build and a baseline, each loaded by
 * an {@link IsolatedBuild} of its own, a round trip of every file of a corpus by each in turn, which goes
 * first alternating, after a warm-up round of each. It prints the minimum and the median of the rounds by
 * the CPU time of the thread and whether the two wrote the same containers. Two runs of
 * {@link PerformanceHarness} on the development machine differ by up to 10% as the clock speed drifts;
 * set side by side like this, two builds resolve to about 2%.
 *
 * <pre>
 * git worktree add ../acb-base COMMIT
 * (cd ../acb-base &amp;&amp; mvn -q compile)
 * mvn -q package -DskipTests
 * java -cp "target/acb.jar;target/test-classes" cz.cvut.fit.acb.harness.SpeedComparison ../acb-base/target/classes DIR [coder=acbx] [rounds=8]
 * </pre>
 *
 * (Use {@code :} instead of {@code ;} outside Windows.)
 */
public final class SpeedComparison {

    private static final String DEFAULT_CODER = "acbx";
    private static final int DEFAULT_ROUNDS = 8;

    /** The rounds of one build: the minimum and median times in seconds, and what it wrote. */
    record Times(double compressMin, double compressMedian, double decompressMin, double decompressMedian,
                 long containerBytes, long crc) {

        static Times of(IsolatedBuild.Round[] rounds) {
            long[] compress = Arrays.stream(rounds).mapToLong(IsolatedBuild.Round::compressNanos).sorted().toArray();
            long[] decompress = Arrays.stream(rounds).mapToLong(IsolatedBuild.Round::decompressNanos).sorted().toArray();
            IsolatedBuild.Round first = rounds[0];
            return new Times(compress[0] / 1e9, compress[compress.length / 2] / 1e9, decompress[0] / 1e9,
                    decompress[decompress.length / 2] / 1e9, first.containerBytes(), first.crc());
        }

        boolean wroteTheSameAs(Times other) {
            return this.containerBytes == other.containerBytes && this.crc == other.crc;
        }
    }

    /** The two builds' times. */
    record Comparison(Times base, Times change) {

        boolean identical() {
            return this.base.wroteTheSameAs(this.change);
        }
    }

    private SpeedComparison() {
    }

    public static void main(String[] args) throws IOException, ReflectiveOperationException, URISyntaxException {
        if (args.length < 2 || args.length > 4) {
            System.err.println("usage: SpeedComparison BASE_CLASSES DIR [coder=" + DEFAULT_CODER + "] [rounds="
                    + DEFAULT_ROUNDS + "]");
            System.exit(2);
        }
        String coder = args.length > 2 ? args[2] : DEFAULT_CODER;
        int rounds = args.length > 3 ? Integer.parseInt(args[3]) : DEFAULT_ROUNDS;
        run(Path.of(args[0]), ownClasses(), Path.of(args[1]), coder, rounds, System.out);
    }

    /** Where this build's coders were loaded from: {@code target/classes} or the jar. */
    static Path ownClasses() throws URISyntaxException {
        return Path.of(Compressor.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    static Comparison run(Path baseClasses, Path changeClasses, Path corpus, String coder, int rounds,
                          PrintStream out) throws IOException, ReflectiveOperationException {
        if (rounds < 1) {
            throw new IllegalArgumentException("At least one round, not " + rounds);
        }
        List<byte[]> files = read(corpus);
        IsolatedBuild base = IsolatedBuild.load(baseClasses, coder);
        IsolatedBuild change = IsolatedBuild.load(changeClasses, coder);
        out.printf(Locale.ROOT, "base   %s%nchange %s%n%s: %d files, %,d bytes; %s, %d rounds after a warm-up, "
                + "CPU time of the thread%n%n", baseClasses, changeClasses, corpus, files.size(),
                files.stream().mapToLong(file -> file.length).sum(), coder, rounds);
        base.round(files);
        change.round(files);
        IsolatedBuild.Round[] baseRounds = new IsolatedBuild.Round[rounds];
        IsolatedBuild.Round[] changeRounds = new IsolatedBuild.Round[rounds];
        for (int round = 0; round < rounds; round++) {
            if (round % 2 == 0) {
                baseRounds[round] = base.round(files);
                changeRounds[round] = change.round(files);
            } else {
                changeRounds[round] = change.round(files);
                baseRounds[round] = base.round(files);
            }
        }
        Comparison comparison = new Comparison(Times.of(baseRounds), Times.of(changeRounds));
        print(comparison, out);
        return comparison;
    }

    private static void print(Comparison comparison, PrintStream out) {
        Times base = comparison.base();
        Times change = comparison.change();
        out.printf(Locale.ROOT, "%-12s %9s %9s %9s %9s  (seconds)%n", "", "comp min", "comp med", "dec min",
                "dec med");
        out.printf(Locale.ROOT, "%-12s %9.3f %9.3f %9.3f %9.3f%n", "base", base.compressMin(),
                base.compressMedian(), base.decompressMin(), base.decompressMedian());
        out.printf(Locale.ROOT, "%-12s %9.3f %9.3f %9.3f %9.3f%n", "change", change.compressMin(),
                change.compressMedian(), change.decompressMin(), change.decompressMedian());
        out.printf(Locale.ROOT, "%-12s %9.3f %9.3f %9.3f %9.3f%n%n", "change/base",
                change.compressMin() / base.compressMin(), change.compressMedian() / base.compressMedian(),
                change.decompressMin() / base.decompressMin(), change.decompressMedian() / base.decompressMedian());
        if (comparison.identical()) {
            out.printf(Locale.ROOT, "the containers are identical: %,d bytes, CRC %08x%n", base.containerBytes(),
                    base.crc());
        } else {
            out.printf(Locale.ROOT, "the containers differ: base %,d bytes (CRC %08x), change %,d bytes (CRC %08x)%n",
                    base.containerBytes(), base.crc(), change.containerBytes(), change.crc());
        }
    }

    private static List<byte[]> read(Path corpus) throws IOException {
        try (Stream<Path> paths = Files.list(corpus)) {
            return paths.filter(Files::isRegularFile).sorted().map(SpeedComparison::readAll).toList();
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
