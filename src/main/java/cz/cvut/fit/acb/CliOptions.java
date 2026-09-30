package cz.cvut.fit.acb;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.Level;

import java.util.Arrays;

/** The options of the command line and the words that describe them in the help. */
final class CliOptions {

    private CliOptions() {
    }

    static Options all() {
        Options options = new Options();
        options.addOption("de", "decompress", false,
                "decompress input (default is to compress); coding settings are read from the file, so the coding options do not apply");
        options.addOption("h", "help", false, "print this help");
        options.addOption("f", "force", false, "overwrite output files that already exist");
        options.addOption("bs", "bit-stream-array", false,
                "no coding is used for triplets (default is adaptive range coding)");
        options.addOption(Option.builder("j")
                .longOpt("threads")
                .hasArg()
                .argName("N")
                .desc("N threads code segments at the same time (default is the number of processors, "
                        + CliRequest.Work.DEFAULT_THREADS + " here); the output does not depend on it")
                .build());
        options.addOption(Option.builder("log")
                .longOpt("log-level")
                .hasArg()
                .argName("level")
                .desc("sets logging level of the application (default is warn)\n"
                        + "values = " + Arrays.toString(Level.values()))
                .build());
        options.addOption(Option.builder("d")
                .longOpt("distance")
                .hasArg()
                .argName("N")
                .desc("N bits, 1 to " + CompressionSettings.MAX_FIELD_BITS
                        + ", used for distance triplet element (default is 6)\n"
                        + "maximal context-content distance is 2^(N - 1); for acb, a funnel of analogies takes "
                        + "2^(N - 1) entries from either side of a context")
                .build());
        options.addOption(Option.builder("l")
                .longOpt("length")
                .hasArg()
                .argName("N")
                .desc("N bits, 1 to " + CompressionSettings.MAX_FIELD_BITS
                        + ", used for length triplet element (default is 7, 8 for acb)\n"
                        + "maximal length is 2^(N)-1")
                .build());
        options.addOption(Option.builder("cd")
                .longOpt("context-depth")
                .hasArg()
                .argName("N")
                .desc("N bytes, 1 to " + CompressionSettings.MAX_CONTEXT_DEPTH
                        + ", of the context before a position decide where it sorts in the dictionary "
                        + "(default is 10, " + CompressionSettings.MAX_CONTEXT_DEPTH + " for acb)")
                .build());
        options.addOption(Option.builder("ec")
                .longOpt("entropy-coder")
                .hasArg()
                .argName("coder")
                .desc("<coder> turns triplet fields into bytes (default is ADAPTIVE_ARITHMETIC)\n"
                        + "CONTEXT_ARITHMETIC codes literals better than the coders define them\n"
                        + "values = " + Arrays.toString(EntropyCoding.values()))
                .build());
        options.addOption(Option.builder("m")
                .longOpt("measure")
                .optionalArg(true)
                .argName("out")
                .desc("measured program process data printed to file <out> or to standard output if no file specified")
                .build());
        options.addOption(Option.builder("af")
                .longOpt("arith-freq")
                .hasArg()
                .argName("freq")
                .desc("<freq> is comma separated array of positive integers defining init values of the range "
                        + "coding frequency table for lengths (default is all 1; symbols past the list start at 1; each coded length adds 32)")
                .build());
        options.addOption(Option.builder("tc")
                .longOpt("triplet-coder")
                .hasArg()
                .argName("coder")
                .desc("<coder> represents triplet coding strategy (default is valach)\n"
                        + "acb is Buyanovsky's own coder and acbx its variant with mixed models: they code with models of their "
                        + "own, so they take no -ec or -bs\n"
                        + "values = " + Arrays.toString(TripletCoding.values()))
                .build());
        return options;
    }
}
