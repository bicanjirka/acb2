package cz.cvut.fit.acb;

import cz.cvut.fit.acb.format.CompressedStream;
import cz.cvut.fit.acb.format.MalformedStreamException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The command line. Exit code 0 means success (and {@code -h}), 1 a failure while working, 2 that
 * the arguments were not understood. Diagnostics go to standard error through the logger; only
 * requested output (help, measurements) goes to standard output.
 */
public final class ACBClient {

    private static final int EXIT_OK = 0;
    private static final int EXIT_FAILURE = 1;
    private static final int EXIT_USAGE = 2;
    private static final Logger LOG = LogManager.getLogger();

    private final CliParser parser = new CliParser();

    public static void main(String[] args) {
        int exitCode;
        try {
            exitCode = new ACBClient().run(args);
        } catch (Exception e) {
            LOG.error("Unexpected failure", e);
            exitCode = EXIT_FAILURE;
        }
        System.exit(exitCode);
    }

    int run(String[] args) {
        CliRequest request;
        try {
            request = this.parser.parse(args);
        } catch (CliParser.UsageException e) {
            LOG.error("{}", e.getMessage());
            this.parser.printHelp(System.err);
            return EXIT_USAGE;
        }
        return switch (request) {
            case CliRequest.Help help -> {
                this.parser.printHelp(System.out);
                yield EXIT_OK;
            }
            case CliRequest.Work work -> this.execute(work);
        };
    }

    private int execute(CliRequest.Work work) {
        work.logLevel().ifPresent(ACBClient::applyLogLevel);
        LOG.info("{} {}", work.mode() == CliRequest.Mode.COMPRESS ? "compressing" : "decompressing", work.input());
        LOG.debug("settings = {}", work.settings());
        try {
            ACBFileIO io = new ACBFileIO();
            FileAction action = work.mode() == CliRequest.Mode.COMPRESS
                    ? compression(io, work.settings()) : decompression(io, work.settings());
            List<String> measurements = new ArrayList<>();
            for (FileJobs.Job job : FileJobs.plan(work)) {
                long start = System.nanoTime();
                action.apply(job.source(), job.target());
                long millis = (System.nanoTime() - start) / 1_000_000;
                measurements.add(measurement(job, millis));
            }
            this.report(work.measure(), measurements);
        } catch (MalformedStreamException e) {
            return failure("Cannot decompress", e);
        } catch (IOException | UncheckedIOException e) {
            return failure("I/O error", e);
        }
        return EXIT_OK;
    }

    private static int failure(String what, Exception cause) {
        LOG.error("{}: {}", what, cause.getMessage());
        LOG.debug("Failure details", cause);
        return EXIT_FAILURE;
    }

    private static void applyLogLevel(Level level) {
        Configurator.setRootLevel(level);
    }

    private void report(CliRequest.Measure measure, List<String> measurements) throws IOException {
        switch (measure) {
            case CliRequest.Measure.Off off -> {
            }
            case CliRequest.Measure.Console console -> measurements.forEach(System.out::println);
            case CliRequest.Measure.ToFile toFile -> Files.write(toFile.file(), measurements);
        }
    }

    private static String measurement(FileJobs.Job job, long millis) throws IOException {
        long sourceSize = Files.size(job.source());
        long targetSize = Files.size(job.target());
        return String.format(Locale.ROOT, "%s\ttime: %d ms\tin: %d B\tout: %d B\tratio: %.4f",
                job.source().getFileName(), millis, sourceSize, targetSize,
                sourceSize == 0 ? 0.0 : (double) targetSize / sourceSize);
    }

    private static FileAction compression(ACBFileIO io, CompressionSettings settings) {
        Compressor compressor = new Compressor(settings);
        return (source, target) -> {
            CompressedStream stream;
            try (SegmentReader segments = io.readSegments(source, settings.segmentSize())) {
                stream = compressor.compress(segments);
            }
            io.saveCompressed(stream, target);
        };
    }

    /** Coding settings come from each file's header; only the dictionary structure is chosen here. */
    private static FileAction decompression(ACBFileIO io, CompressionSettings settings) {
        Compressor compressor = new Compressor(settings);
        return (source, target) -> {
            CompressedStream stream = io.openCompressed(source);
            try (SegmentWriter segments = io.writeSegments(target)) {
                compressor.decompress(stream, segments);
                segments.commit();
            }
        };
    }

    private interface FileAction {
        void apply(Path source, Path target) throws IOException;
    }
}
