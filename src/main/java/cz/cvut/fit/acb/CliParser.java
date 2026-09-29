package cz.cvut.fit.acb;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serial;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

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

    private final Options options = new Options();

    CliParser() {
        this.options.addOption("de", "decompress", false,
                "decompress input (default is to compress); coding settings are read from the file, so the coding options do not apply");
        this.options.addOption("h", "help", false, "print this help");
        this.options.addOption("f", "force", false, "overwrite output files that already exist");
        this.options.addOption("bs", "bit-stream-array", false,
                "no coding is used for triplets (default is adaptive range coding)");
        this.options.addOption(Option.builder("log")
                .longOpt("log-level")
                .hasArg()
                .argName("level")
                .desc("sets logging level of the application (default is warn)\n"
                        + "values = " + Arrays.toString(Level.values()))
                .build());
        this.options.addOption(Option.builder("d")
                .longOpt("distance")
                .hasArg()
                .argName("N")
                .desc("N bits, 1 to " + CompressionSettings.MAX_FIELD_BITS
                        + ", used for distance triplet element (default is 6)\n"
                        + "maximal context-content distance is 2^(N - 1)")
                .build());
        this.options.addOption(Option.builder("l")
                .longOpt("length")
                .hasArg()
                .argName("N")
                .desc("N bits, 1 to " + CompressionSettings.MAX_FIELD_BITS
                        + ", used for length triplet element (default is 7)\n"
                        + "maximal length is 2^(N)-1")
                .build());
        this.options.addOption(Option.builder("cd")
                .longOpt("context-depth")
                .hasArg()
                .argName("N")
                .desc("N bytes, 1 to " + CompressionSettings.MAX_CONTEXT_DEPTH
                        + ", of the context before a position decide where it sorts in the dictionary (default is 10)")
                .build());
        this.options.addOption(Option.builder("ec")
                .longOpt("entropy-coder")
                .hasArg()
                .argName("coder")
                .desc("<coder> turns triplet fields into bytes (default is ADAPTIVE_ARITHMETIC)\n"
                        + "CONTEXT_ARITHMETIC codes literals better than the coders define them\n"
                        + "values = " + Arrays.toString(EntropyCoding.values()))
                .build());
        this.options.addOption(Option.builder("m")
                .longOpt("measure")
                .optionalArg(true)
                .argName("out")
                .desc("measured program process data printed to file <out> or to standard output if no file specified")
                .build());
        this.options.addOption(Option.builder("af")
                .longOpt("arith-freq")
                .hasArg()
                .argName("freq")
                .desc("<freq> is comma separated array of positive integers defining init values of the range "
                        + "coding frequency table for lengths (default is all 1; symbols past the list start at 1; each coded length adds 32)")
                .build());
        this.options.addOption(Option.builder("tc")
                .longOpt("triplet-coder")
                .hasArg()
                .argName("coder")
                .desc("<coder> represents triplet coding strategy (default is valach)\n"
                        + "values = " + Arrays.toString(TripletCoding.values()))
                .build());
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
            String file = cmd.getOptionValue("m");
            work = work.withMeasure(file == null ? CliRequest.Measure.console()
                    : CliRequest.Measure.toFile(Path.of(file)));
        }
        if (cmd.hasOption("log")) {
            work = work.withLogLevel(parseLevel(cmd.getOptionValue("log")));
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
        CompressionSettings settings = CompressionSettings.defaults();
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
        if (cmd.hasOption("tc")) {
            TripletCoding coding = parseEnum(TripletCoding.class, cmd.getOptionValue("tc"), "triplet-coder");
            settings = settings.withTripletCoding(coding);
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
