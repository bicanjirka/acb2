package cz.cvut.fit.acb;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import cz.cvut.fit.acb.format.CompressedStream;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.format.StreamHeader;
import cz.cvut.fit.acb.utils.ChainBuilder;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

/**
 * @author jiri.bican
 */
public class ACBClient {
	
	private static final int EXIT_CODE_OK = 0;
	private static final int EXIT_CODE_FATAL = 1;
	private static final int EXIT_CODE_HELP = 2;
	private static final Options options = new Options();
	private static final Logger logger = LogManager.getLogger();

	static {
		options.addOption("de", "decompress", false, "decompress input (default is to compress); coding settings are read from the file, so only -ds applies");
		options.addOption("h", "help", false, "print this help");
		options.addOption("bs", "bit-stream-array", false, "no coding is used for triplets (default is adaptive arithmetic coding)");
		Option logger1 = Option.builder("log")
				.longOpt("log-level")
				.hasArg()
				.argName("level")
				.desc("sets logging level of the application")
				.build();
		options.addOption(logger1);
		Option distance = Option.builder("d")
				.longOpt("distance")
				.hasArg()
				.argName("N")
				.desc("N bits used for distance triplet element (default is 6)\n" +
						"maximal context-content distance is 2^(N - 1)")
				.build();
		options.addOption(distance);
		Option length = Option.builder("l")
				.longOpt("length")
				.hasArg()
				.argName("N")
				.desc("N bits used for length triplet element (default is 4)\n" +
						"maximal length is 2^(N)-1")
				.build();
		options.addOption(length);
		Option metrics = Option.builder("m")
				.longOpt("measure")
				.optionalArg(true)
				.argName("out")
				.desc("measured program process data printed to file <out> or to standard output if no file specified")
				.build();
		options.addOption(metrics);
		Option freq = Option.builder("af")
				.longOpt("arith-freq")
				.hasArg()
				.argName("freq")
				.desc("<freq> is comma separated array of positive integers defining init values of the arithmetic coding frequency table for lengths (default is 45,13,10,7,5,4, then 1 for the rest)")
				.build();
		options.addOption(freq);
		Option tripCoder = Option.builder("tc")
				.longOpt("triplet-coder")
				.hasArg()
				.argName("coder")
				.desc("<coder> represents triplet coding strategy (default is simple)\n" +
						"values = " + Arrays.toString(TripletCoding.values()))
				.build();
		options.addOption(tripCoder);
		Option struct = Option.builder("ds")
				.longOpt("dict-struct")
				.hasArg()
				.argName("struct")
				.desc("<struct> represents data structure used in dictionary (default is red_black)\n" +
						"values = " + Arrays.toString(DictionaryStructure.values()))
				.build();
		options.addOption(struct);
	}
	
	private String input;
	private String output;
	private boolean compress = true;
	private boolean measure;
	private Optional<String> measureOutput = Optional.empty();
	
	public static void main(String[] args) {
		
		try {
			ACBClient app = new ACBClient();
			int exitCode = app.run(args);
			if (exitCode != EXIT_CODE_OK) {
				if (exitCode == EXIT_CODE_HELP) {
					printHelp();
					return;
				}
				System.exit(exitCode);
			}
			
		} catch (Exception e) {
			logger.error("Unexpected failure", e);
			System.exit(EXIT_CODE_FATAL);
		}
	}
	
	private static void printHelp() {
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("acb.jar input output [options]\n" +
				"input - input file or directory", options);
	}
	
	int run(String[] args) {
		if (args.length < 2) {
			return EXIT_CODE_HELP;
		}
		
		CommandLineParser parser = new DefaultParser();
		CompressionSettings settings;
		try {
			CommandLine cmd = parser.parse(options, args);
			if (cmd.hasOption('h')) {
				return EXIT_CODE_HELP;
			}
			settings = parseCommandLine(cmd);
		} catch (ParseException exp) {
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			logger.error("Parsing failed.  Reason: {}", exp.getMessage());
			return EXIT_CODE_FATAL;
		}
		
		logger.info("{} {}", this.compress ? "compressing" : "decompressing", this.input);
		logger.debug("settings = {}", settings);
		logger.debug("measuring is {}", this.measure ? "ON, output into " + this.measureOutput.orElse("console") : "OFF");
		
		ACBFileIO io = new ACBFileIO();
		FileAction action = this.compress ? compression(io, settings) : decompression(io, settings.dictionaryStructure());
		
		try {
			List<String> measurements = new ArrayList<>();
			for (FileJob job : this.jobs(Paths.get(this.input), Paths.get(this.output))) {
				long start = System.nanoTime();
				action.apply(job.source(), job.target());
				long millis = (System.nanoTime() - start) / 1_000_000;
				long sourceSize = Files.size(job.source());
				long targetSize = Files.size(job.target());
				measurements.add(String.format(Locale.ROOT, "%s	time: %d ms	in: %d B	out: %d B	ratio: %.4f",
						job.source().getFileName(), millis, sourceSize, targetSize,
						sourceSize == 0 ? 0.0 : (double) targetSize / sourceSize));
			}
			if (this.measure) {
				if (this.measureOutput.isPresent()) {
					Files.write(Paths.get(this.measureOutput.get()), measurements);
				} else {
					measurements.forEach(System.out::println);
				}
			}
		} catch (MalformedStreamException exp) {
			System.err.println("Cannot decompress.  Reason: " + exp.getMessage());
			logger.error("Cannot decompress.  Reason: {}", exp.getMessage());
			return EXIT_CODE_FATAL;
		} catch (IOException | UncheckedIOException exp) {
			System.err.println("I/O Exception.  Reason: " + exp.getMessage());
			logger.error("I/O Exception.  Reason: {}", exp.getMessage());
			return EXIT_CODE_FATAL;
		}
		return EXIT_CODE_OK;
	}
	
	/**
	 * A file input maps to the output path, or into it when it is a directory; a directory input
	 * maps each regular file inside it to the same name in the output directory.
	 */
	private List<FileJob> jobs(Path in, Path out) throws IOException {
		if (!Files.exists(in)) {
			throw new IOException("Input file or directory does not exist: " + in);
		}
		List<FileJob> jobs = new ArrayList<>();
		if (Files.isDirectory(in)) {
			if (Files.exists(out) && !Files.isDirectory(out)) {
				throw new IOException("Input is a directory, so the output must be one too: " + out);
			}
			Files.createDirectories(out);
			try (Stream<Path> files = Files.list(in)) {
				files.filter(Files::isRegularFile).sorted()
						.forEach(file -> jobs.add(new FileJob(file, out.resolve(file.getFileName()))));
			}
		} else {
			jobs.add(new FileJob(in, Files.isDirectory(out) ? out.resolve(in.getFileName()) : out));
		}
		for (FileJob job : jobs) {
			if (Files.exists(job.target()) && Files.isSameFile(job.source(), job.target())) {
				throw new IOException("Output would overwrite its input: " + job.target());
			}
		}
		return jobs;
	}
	
	private static FileAction compression(ACBFileIO io, CompressionSettings settings) {
		ACBProvider provider = new ACBProviderImpl(settings);
		ACB acb = new ACB(provider);
		StreamHeader header = StreamHeader.of(settings);
		return (source, target) -> ChainBuilder.create(io::openParse)
				.chain(acb::compress)
				.chain(provider.getT2BConverter())
				.end(payload -> io.saveCompressed(new CompressedStream(header, payload), target))
				.accept(source);
	}
	
	/** Coding settings come from each file's header; only the dictionary structure is chosen here. */
	private static FileAction decompression(ACBFileIO io, DictionaryStructure structure) {
		return (source, target) -> {
			CompressedStream stream = io.openCompressed(source);
			ACBProvider provider = new ACBProviderImpl(stream.header().toSettings(structure));
			ACB acb = new ACB(provider);
			ChainBuilder.create(provider.getB2TConverter())
					.chain(acb::decompress)
					.end(io.parsedWriter(target))
					.accept(stream.payload());
		};
	}
	
	private interface FileAction {
		void apply(Path source, Path target) throws IOException;
	}
	
	private record FileJob(Path source, Path target) {
	}
	
	private CompressionSettings parseCommandLine(CommandLine cmd) throws ParseException {
		String[] args = cmd.getArgs();
		if (args == null || args.length != 2) {
			throw new ParseException("Bad usage: arguments [input, output] required, found: " + Arrays.toString(args));
		}
		this.input = args[0];
		this.output = args[1];
		this.compress = !cmd.hasOption("de");
		
		if (cmd.hasOption("log")) {
			String val = cmd.getOptionValue("log");
			Level level;
			try {
				level = Level.valueOf(val);
			} catch (IllegalArgumentException e) {
				throw new ParseException("Invalid log level: " + val + ", allowed values: " + Arrays.toString(Level.values()));
			}
			LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
			Configuration config = ctx.getConfiguration();
			LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
			loggerConfig.setLevel(level);
			ctx.updateLoggers();
			logger.info("Log level changed to {}", level.name());
		}
		
		if (cmd.hasOption("m")) {
			this.measure = true;
			this.measureOutput = Optional.ofNullable(cmd.getOptionValue("m"));
		}
		
		CompressionSettings settings = CompressionSettings.defaults();
		try {
			if (cmd.hasOption("d")) {
				settings = settings.withDistanceBits(parseInt(cmd.getOptionValue("d"), "distance"));
			}
			if (cmd.hasOption("l")) {
				settings = settings.withLengthBits(parseInt(cmd.getOptionValue("l"), "length"));
			}
			if (cmd.hasOption("bs")) {
				settings = settings.withEntropyCoding(EntropyCoding.BIT_ARRAY);
			}
			if (cmd.hasOption("af")) {
				String val = cmd.getOptionValue("af");
				try {
					settings = settings.withLengthFrequencies(
							Arrays.stream(val.split(",")).map(String::trim).mapToInt(Integer::parseInt).toArray());
				} catch (NumberFormatException e) {
					throw new ParseException("arith-freq is not a comma separated list of integers: " + val);
				}
			}
			if (cmd.hasOption("tc")) {
				TripletCoding coding = parseEnum(TripletCoding.class, cmd.getOptionValue("tc"), "triplet-coder");
				if (coding == TripletCoding.LCP) {
					throw new ParseException("triplet-coder LCP is experimental and its output does not decompress yet");
				}
				settings = settings.withTripletCoding(coding);
			}
			if (cmd.hasOption("ds")) {
				settings = settings.withDictionaryStructure(
						parseEnum(DictionaryStructure.class, cmd.getOptionValue("ds"), "dictionary-structure"));
			}
		} catch (IllegalArgumentException e) {
			throw new ParseException(e.getMessage());
		}
		return settings;
	}
	
	private static int parseInt(String val, String what) throws ParseException {
		try {
			return Integer.parseInt(val);
		} catch (NumberFormatException e) {
			throw new ParseException(what + " is not a number: " + val);
		}
	}
	
	private static <E extends Enum<E>> E parseEnum(Class<E> type, String val, String what) throws ParseException {
		for (E constant : type.getEnumConstants()) {
			if (constant.name().equalsIgnoreCase(val)) {
				return constant;
			}
		}
		throw new ParseException(what + " invalid: " + val + ", allowed values: " + Arrays.toString(type.getEnumConstants()));
	}
}
