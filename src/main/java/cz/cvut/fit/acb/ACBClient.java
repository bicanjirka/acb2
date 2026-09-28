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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

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
		options.addOption("de", "decompress", false, "decompress input (default is to compress)");
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
				.desc("<freq> is comma separated array of positive integers defining init values of the arithmetic coding frequency table for lengths (default is 45,13,10,7,5,4, then 1 for the rest); decompress with the same values")
				.build();
		options.addOption(freq);
		Option tripCoder = Option.builder("tc")
				.longOpt("triplet-coder")
				.hasArg()
				.argName("coder")
				.desc("<coder> represents triplet coding strategy (default is simple)\n" +
						"values = " + Arrays.toString(ACBProviderParameters.TripletCoderE.values()))
				.build();
		options.addOption(tripCoder);
		Option struct = Option.builder("ds")
				.longOpt("dict-struct")
				.hasArg()
				.argName("struct")
				.desc("<struct> represents data structure used in dictionary (default is red_black)\n" +
						"values = " + Arrays.toString(ACBProviderParameters.OrderStatisticTreeE.values()))
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
			
		} catch (Throwable t) {
			t.printStackTrace();
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
		ACBProviderParameters params;
		try {
			CommandLine cmd = parser.parse(options, args);
			if (cmd.hasOption('h')) {
				return EXIT_CODE_HELP;
			}
			params = parseCommandLine(cmd);
		} catch (ParseException exp) {
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			logger.error("Parsing failed.  Reason: {}", exp.getMessage());
			return EXIT_CODE_FATAL;
		}
		
		logger.info("{} {}", this.compress ? "compressing" : "decompressing", this.input);
		logger.debug("distance bits = {}", params.distanceBits);
		logger.debug("length bits = {}", params.lengthBits);
		logger.debug("triplet coding = {}", params.tc.name());
		logger.debug("dictionary structure = {}", params.tr.name());
		logger.debug("triplet coder = {}", params.cd.name());
		logger.debug("measuring is {}", this.measure ? "ON, output into " + this.measureOutput.orElse("console") : "OFF");
		
		ACBFileIO io = new ACBFileIO();
		ACBProvider provider = new ACBProviderImpl(params);
		ACB acb = new ACB(provider);
		
		Function<Path, Consumer<Path>> chain;
		if (this.compress) {
			chain = output -> ChainBuilder.create(io::openParse)
					.chain(acb::compress)
					.chain(provider.getT2BConverter())
					.end(bytes -> io.saveObject(bytes, output));
		} else {
			chain = output -> ChainBuilder.create(io::openObject)
					.chain(provider.getB2TConverter())
					.chain(acb::decompress)
					.end(io.parsedWriter(output));
		}
		
		try {
			List<String> measurements = new ArrayList<>();
			for (FileJob job : this.jobs(Paths.get(this.input), Paths.get(this.output))) {
				long start = System.nanoTime();
				chain.apply(job.target()).accept(job.source());
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
	
	private record FileJob(Path source, Path target) {
	}
	
	private ACBProviderParameters parseCommandLine(CommandLine cmd) throws ParseException {
		ACBProviderParameters params = new ACBProviderParameters();
		String[] args = cmd.getArgs();
		if (args == null || args.length != 2) {
			throw new ParseException("Bad usage: arguments [input, output] required, found: " + Arrays.toString(args));
		}
		input = args[0];
		output = args[1];
		
		if (cmd.hasOption("de")) {
			compress = false;
		}
		
		if (cmd.hasOption("log")) {
			String val = cmd.getOptionValue("log");
			LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
			Configuration config = ctx.getConfiguration();
			LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
			Level level = null;
			try {
				level = Level.valueOf(val);
			} catch (Exception e) {
				throw new ParseException("Invalid log level: " + e.getMessage() + "\nallowed constants = " + Arrays.toString(Level.values()));
			}
			loggerConfig.setLevel(level);
			ctx.updateLoggers();  // This causes all Loggers to refetch information from their LoggerConfig.
			logger.info("Log level changed to " + level.name());
		}
		
		if (cmd.hasOption("d")) {
			String val = cmd.getOptionValue("d");
			int d;
			try {
				d = Integer.parseInt(val);
				if (d <= 0) {
					throw new ParseException("distance must be greater than zero: " + val);
				}
			} catch (NumberFormatException e) {
				throw new ParseException("distance is not a number: " + val);
			}
			params.distanceBits = d;
		}
		
		if (cmd.hasOption("l")) {
			String val = cmd.getOptionValue("l");
			int l;
			try {
				l = Integer.parseInt(val);
				if (l <= 0) {
					throw new ParseException("length must be greater than zero: " + val);
				}
			} catch (NumberFormatException e) {
				throw new ParseException("length is not a number: " + val);
			}
			params.lengthBits = l;
		}
		
		if (cmd.hasOption("bs")) {
			params.cd = ACBProviderParameters.CoderE.BIT_ARRAY;
		} else {
			params.cd = ACBProviderParameters.CoderE.ADAPTIVE_ARITHMETIC;
		}
		
		if (cmd.hasOption("af")) {
			String val = cmd.getOptionValue("af");
			try {
				params.lengthFrequencies = Arrays.stream(val.split(",")).map(String::trim).mapToInt(Integer::parseInt).toArray();
			} catch (NumberFormatException e) {
				throw new ParseException("arith-freq is not a comma separated list of integers: " + val);
			}
			if (Arrays.stream(params.lengthFrequencies).anyMatch(f -> f <= 0)) {
				throw new ParseException("arith-freq values must be greater than zero: " + val);
			}
		}
		
		if (cmd.hasOption("m")) {
			measure = true;
			measureOutput = Optional.ofNullable(cmd.getOptionValue("m"));
		}
		
		if (cmd.hasOption("tc")) {
			String val = cmd.getOptionValue("tc");
			ACBProviderParameters.TripletCoderE e = Arrays.stream(ACBProviderParameters.TripletCoderE.values())
					.filter(e1 -> e1.name().equalsIgnoreCase(val)).findAny().orElse(null);
			if (e == null) {
				throw new ParseException("triplet-coder invalid: " + val + ", allowed values: " + Arrays.toString(ACBProviderParameters.TripletCoderE.values()));
			}
			if (e == ACBProviderParameters.TripletCoderE.LCP) {
				throw new ParseException("triplet-coder LCP is experimental and its output does not decompress yet");
			}
			params.tc = e;
		}
		
		if (cmd.hasOption("ds")) {
			String val = cmd.getOptionValue("ds");
			ACBProviderParameters.OrderStatisticTreeE e = Arrays.stream(ACBProviderParameters.OrderStatisticTreeE.values())
					.filter(e1 -> e1.name().equalsIgnoreCase(val)).findAny().orElse(null);
			if (e == null) {
				throw new ParseException("dictionary-structure invalid: " + val + ", allowed values: " + Arrays.toString(ACBProviderParameters.OrderStatisticTreeE.values()));
			}
			params.tr = e;
		}
		
		return params;
	}
}
