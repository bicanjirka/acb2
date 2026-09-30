package cz.cvut.fit.acb;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serial;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** Turns command-line arguments into a {@link CliRequest}; touches neither files nor the console. */
final class CliParser {

    private static final int HELP_WIDTH = 100;

    /** The arguments cannot be a request. */
    static final class UsageException extends Exception {

        @Serial
        private static final long serialVersionUID = 1L;

        UsageException(String message) {
            super(message);
        }
    }

    private final Options options;

    CliParser() {
        this.options = CliOptions.all();
    }

    CliRequest parse(String[] args) throws UsageException {
        CommandLine cmd;
        try {
            cmd = new DefaultParser().parse(this.options, args);
        } catch (ParseException e) {
            throw new UsageException(e.getMessage());
        }
        if (cmd.hasOption("h")) {
            return new CliRequest.Help();
        }
        String[] paths = cmd.getArgs();
        if (paths.length != 2) {
            throw new UsageException("Arguments [input, output] required, found: " + Arrays.toString(paths));
        }
        CliRequest.Work work = CliRequest.Work.of(Path.of(paths[0]), Path.of(paths[1]))
                .withMode(cmd.hasOption("de") ? CliRequest.Mode.DECOMPRESS : CliRequest.Mode.COMPRESS)
                .withForce(cmd.hasOption("f"));
        if (cmd.hasOption("m")) {
            work = work.withMeasure(Optional.ofNullable(cmd.getOptionValue("m")).map(Path::of)
                    .map(CliRequest.Measure::toFile).orElseGet(CliRequest.Measure::console));
        }
        if (cmd.hasOption("log")) {
            work = work.withLogLevel(parseLevel(cmd.getOptionValue("log")));
        }
        if (cmd.hasOption("j")) {
            int threads = parseInt(cmd.getOptionValue("j"), "threads");
            if (threads < 1) {
                throw new UsageException("threads must be at least one: " + threads);
            }
            work = work.withThreads(threads);
        }
        try {
            return work.withSettings(this.settings(cmd));
        } catch (IllegalArgumentException e) {
            throw new UsageException(e.getMessage());
        }
    }

    void printHelp(PrintStream out) {
        PrintWriter writer = new PrintWriter(out, true);
        new HelpFormatter().printHelp(writer, HELP_WIDTH, "acb.jar input output [options]",
                "input - input file or directory", this.options, 1, 3, null);
        writer.flush();
    }

    private CompressionSettings settings(CommandLine cmd) throws UsageException {
        TripletCoding coding = cmd.hasOption("tc")
                ? parseEnum(TripletCoding.class, cmd.getOptionValue("tc"), "triplet-coder")
                : CompressionSettings.defaults().tripletCoding();
        CompressionSettings settings = CompressionSettings.defaultsFor(coding);
        if (cmd.hasOption("d")) {
            settings = settings.withDistanceBits(parseInt(cmd.getOptionValue("d"), "distance"));
        }
        if (cmd.hasOption("l")) {
            settings = settings.withLengthBits(parseInt(cmd.getOptionValue("l"), "length"));
        }
        if (cmd.hasOption("cd")) {
            settings = settings.withContextDepth(parseInt(cmd.getOptionValue("cd"), "context depth"));
        }
        if (cmd.hasOption("ec")) {
            settings = settings.withEntropyCoding(
                    parseEnum(EntropyCoding.class, cmd.getOptionValue("ec"), "entropy-coder"));
        }
        if (cmd.hasOption("bs")) {
            settings = settings.withEntropyCoding(EntropyCoding.BIT_ARRAY);
        }
        if (cmd.hasOption("af")) {
            String value = cmd.getOptionValue("af");
            try {
                settings = settings.withLengthFrequencies(
                        Arrays.stream(value.split(",")).map(String::trim).mapToInt(Integer::parseInt).toArray());
            } catch (NumberFormatException e) {
                throw new UsageException("arith-freq is not a comma separated list of integers: " + value);
            }
        }
        return settings;
    }

    private static Level parseLevel(String value) throws UsageException {
        try {
            return Level.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new UsageException("Invalid log level: " + value + ", allowed values: "
                    + Arrays.toString(Level.values()));
        }
    }

    private static int parseInt(String value, String what) throws UsageException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new UsageException(what + " is not a number: " + value);
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String what)
            throws UsageException {
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(value)) {
                return constant;
            }
        }
        throw new UsageException(what + " invalid: " + value + ", allowed values: "
                + Arrays.toString(type.getEnumConstants()));
    }
}
