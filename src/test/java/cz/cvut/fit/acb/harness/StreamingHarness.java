package cz.cvut.fit.acb.harness;

import cz.cvut.fit.acb.ACBFileIO;
import cz.cvut.fit.acb.CompressedWriter;
import cz.cvut.fit.acb.CompressionSettings;
import cz.cvut.fit.acb.Compressor;
import cz.cvut.fit.acb.ConfiguredACBProvider;
import cz.cvut.fit.acb.OrderedMapper;
import cz.cvut.fit.acb.SegmentReader;
import cz.cvut.fit.acb.fixtures.GeneratedInput;
import cz.cvut.fit.acb.format.ContainerReader;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.format.StreamHeader;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.CRC32;

/**
 * Streams a generated file, by default a little over 2 GiB, through compression to a file and back
 * through decompression, on a pool of threads, holding a few segments at a time. The decoded
 * segments are never stored: their length and CRC-32 are compared with the input's. Run it with a
 * small heap, which is what shows the file size is not bounded by it, and expect minutes:
 *
 * <pre>
 * mvn -q package -DskipTests
 * java -Xmx512m -cp "target/acb.jar;target/test-classes" cz.cvut.fit.acb.harness.StreamingHarness [size=2200000000] [threads=8] [segment=1000000] [dir=TMP]
 * </pre>
 *
 * (Use {@code :} instead of {@code ;} outside Windows.) The input is one seeded text of a segment's
 * size, repeated, so every block is the same work; it goes in {@code dir}, which needs the input
 * and the compressed file, and both are deleted afterwards. Exits non-zero when the round trip
 * changes the input.
 */
public final class StreamingHarness {

    private static final long DEFAULT_SIZE = 2_200_000_000L;

    /** What one run streamed and whether it came back the same. */
    record Result(long inputBytes, long compressedBytes, int blocks, boolean identical, double compressSeconds,
                  double decompressSeconds) {
    }

    private StreamingHarness() {
    }

    public static void main(String[] args) throws IOException, MalformedStreamException {
        long size = DEFAULT_SIZE;
        int threads = Runtime.getRuntime().availableProcessors();
        int segment = CompressionSettings.defaults().segmentSize();
        Path dir = Files.createTempDirectory("acb-streaming");
        for (String arg : args) {
            String[] keyValue = arg.split("=", 2);
            switch (keyValue[0]) {
                case "size" -> size = Long.parseLong(keyValue[1]);
                case "threads" -> threads = Integer.parseInt(keyValue[1]);
                case "segment" -> segment = Integer.parseInt(keyValue[1]);
                case "dir" -> dir = Files.createDirectories(Path.of(keyValue[1]));
                default -> throw new IllegalArgumentException("Unknown option " + arg);
            }
        }
        Result result = run(size, threads, segment, dir, System.out);
        System.exit(result.identical() ? 0 : 1);
    }

    static Result run(long size, int threads, int segmentSize, Path dir, PrintStream out)
            throws IOException, MalformedStreamException {
        CompressionSettings settings = CompressionSettings.defaults().withSegmentSize(segmentSize);
        Path input = dir.resolve("input");
        Path compressed = dir.resolve("input.acb");
        ACBFileIO io = new ACBFileIO();
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            Compressor compressor = new Compressor(settings, ConfiguredACBProvider::new,
                    OrderedMapper.on(pool, 2 * threads));
            out.printf(Locale.ROOT, "%,d bytes in segments of %,d on %d threads, heap limit %,d MiB%n", size,
                    segmentSize, threads, Runtime.getRuntime().maxMemory() >> 20);
            CRC32 written = new CRC32();
            writeInput(input, size, segmentSize, written);

            long start = System.nanoTime();
            try (SegmentReader segments = io.readSegments(input, segmentSize);
                 CompressedWriter blocks = io.createCompressed(compressed, StreamHeader.of(settings),
                         segments.segmentCount())) {
                compressor.compressInto(segments, blocks);
                blocks.commit();
            }
            long middle = System.nanoTime();
            CRC32 decoded = new CRC32();
            long[] decodedBytes = {0};
            int blockCount;
            try (ContainerReader blocks = io.readCompressed(compressed)) {
                blockCount = blocks.blockCount();
                compressor.decompress(blocks.header(), blocks, segment -> {
                    decoded.update(segment);
                    decodedBytes[0] += segment.length;
                });
            }
            long end = System.nanoTime();

            Result result = new Result(size, Files.size(compressed), blockCount,
                    decodedBytes[0] == size && decoded.getValue() == written.getValue(), (middle - start) / 1e9,
                    (end - middle) / 1e9);
            out.printf(Locale.ROOT, "%,d bytes into %,d in %d blocks: compressed in %.1f s, decompressed in %.1f s, %s%n",
                    result.inputBytes(), result.compressedBytes(), result.blocks(), result.compressSeconds(),
                    result.decompressSeconds(), result.identical() ? "identical" : "DIFFERENT");
            return result;
        } finally {
            Files.deleteIfExists(input);
            Files.deleteIfExists(compressed);
        }
    }

    private static void writeInput(Path path, long size, int segmentSize, CRC32 checksum) throws IOException {
        byte[] segment = GeneratedInput.text(segmentSize).bytes();
        try (OutputStream file = Files.newOutputStream(path)) {
            for (long left = size; left > 0; left -= segmentSize) {
                int length = (int) Math.min(segmentSize, left);
                file.write(segment, 0, length);
                checksum.update(segment, 0, length);
            }
        }
    }
}
