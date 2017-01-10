package cz.cvut.fit.acb;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

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
		System.setProperty("log4j.configurationFile", "log4j2.xml");
	}
	
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
				.desc("<freq> is comma separated array of integers defining init values of arithmetic coding frequency table (default is 45,13,10,7,5,4,1...)")
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
	
	private String ìnput;
	private String output;
	private boolean compress = true;
	private boolean measure;
	private String measureOutput;
	
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
	
	private int run(String[] args) {
		if (args.length < 2) {
			return EXIT_CODE_HELP;
		}
		
		// create the parser
		CommandLineParser parser = new DefaultParser();
		ACBProviderParameters params = null;
		try {
			// parse the command line arguments
			CommandLine cmd = parser.parse(options, args);
			
			if (cmd == null || cmd.hasOption('h')) {
				return EXIT_CODE_HELP;
			}
			
			params = parseCommandLine(cmd);
			
		} catch (ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			logger.error("Parsing failed.  Reason: " + exp.getMessage());
			return ACBClient.EXIT_CODE_FATAL;
		}
		
		assert params != null;
		logger.info("{} {}", compress ? "compressing" : "decompressing", ìnput);
		logger.debug("distance bits = {}", params.distanceBits);
		logger.debug("length bits = {}", params.lengthBits);
		logger.debug("triplet coding = {}", params.tc.name());
		logger.debug("dictionary structure = {}", params.tr.name());
		logger.debug("triplet coder = {}", params.cd.name());
		if (measure) {
			logger.debug("measuring is ON, output into {}", "console"); // TODO console - file
		} else {
			logger.debug("measuring is OFF");
		}
		
		ACBFileIO io = new ACBFileIO();
		ACBProvider provider = new ACBProviderImpl(params);
		ACB acb = new ACB(provider);
		
		Function<Path, Consumer<Path>> chain;
		
		if (compress) {
			chain = output -> ChainBuilder.create(io::openParse)
					.chain(acb::compress)
					.chain(provider.getT2BConverter())
					.end(bytes -> io.saveObject(bytes, output));
		} else {
			chain = output -> ChainBuilder.create(io::openObject)
					.chain(provider.getB2TConverter())
					.chain(acb::decompress)
					.end(byteBuffer -> io.saveParsed(byteBuffer, output));
		}
		
		Path in = Paths.get(ìnput);
		Path out = Paths.get(output);
		File[] inFiles = new File[0];
		
		try {
			Files.deleteIfExists(out);
			if (!Files.exists(in)) {
				throw new IOException("Input file or directory does not exists: " + this.ìnput);
			}
			File file = in.toFile();
			if (file.isDirectory()) {
				inFiles = file.listFiles();
			} else if (file.isFile()) {
				inFiles = new File[]{file};
			} else {
				// do nothing
			}
		} catch (IOException exp) {
			System.err.println("I/O Exception.  Reason: " + exp.getMessage());
			logger.error("I/O Exception.  Reason: " + exp.getMessage());
			return ACBClient.EXIT_CODE_FATAL;
		}
		try {
			for (int i = 0; i < inFiles.length; i++) {
				File file = inFiles[i];
//				File outf = File.createTempFile(file.getName(), String.valueOf(i));
				long t1 = System.currentTimeMillis();
				long s1 = file.length();
				chain.apply(file.toPath()).accept(in);
				long t2 = System.currentTimeMillis();
				long s2 = file.length();
				Thread.sleep(500);
				if (measure) System.out.println(file.getName() + "\ttime: " + (t2 - t1) + "\tcompress:" + ((double) s2 / s1));
			}
		} catch (InterruptedException ex) {
//			ex.printStackTrace();
		}
		/*TripletToByteConverter<?> encoder = provider.getT2BConverter();
		ChainBuilder.create(io::openParse)
				.chain(acb::compress)
				.chain(encoder)
				.end(bytes -> io.saveObject(bytes, out))
				.accept(in);
		
		//////////////////////////////////////////////////////
		
		ChainBuilder.create(io::openObject)
				.chain(provider.getB2TConverter())
				.chain(acb::decompress)
				.end(byteBuffer -> {
					try {
						if (byteBuffer == null) return;
						byte[] inBytes = Files.readAllBytes(in);
						byte[] outBytes = byteBuffer.array();
//					logger.info("Input  = " + new String(inBytes));
//					logger.info("Output = " + new String(outBytes));
						logger.info("Input Output equals = {}", Arrays.equals(inBytes, outBytes));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}).accept(out);*/
		
		return EXIT_CODE_OK;
	}
	
	private ACBProviderParameters parseCommandLine(CommandLine cmd) throws ParseException {
		ACBProviderParameters params = new ACBProviderParameters();
		String[] args = cmd.getArgs();
		if (args == null || args.length != 2) {
			throw new ParseException("Bad usage: arguments [input, output] required, found: " + Arrays.toString(args));
		}
		ìnput = args[0];
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
			String[] split = val.split(",");
			// TODO handle frequencies
		}
		
		if (cmd.hasOption("m")) {
			measure = true;
			String val = cmd.getOptionValue("m");
			measureOutput = val;
		}
		
		if (cmd.hasOption("tc")) {
			String val = cmd.getOptionValue("tc");
			ACBProviderParameters.TripletCoderE e = Arrays.stream(ACBProviderParameters.TripletCoderE.values())
					.filter(e1 -> e1.name().equalsIgnoreCase(val)).findAny().orElse(null);
			if (e == null) {
				throw new ParseException("triplet-coder invalid: " + val + ", allowed values: " + Arrays.toString(ACBProviderParameters.TripletCoderE.values()));
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
