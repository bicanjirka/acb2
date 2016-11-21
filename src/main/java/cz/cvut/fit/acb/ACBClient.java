package cz.cvut.fit.acb;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import cz.cvut.fit.acb.coding.TripletToByteConverter;
import cz.cvut.fit.acb.utils.ChainBuilder;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jiri.bican
 */
public class ACBClient {
	
	private static final int EXIT_CODE_OK = 0;
	private static final int EXIT_CODE_FATAL = 2;
	private static final Logger logger = LogManager.getLogger();
	static String in = "corpuses/mailflder corpuses/out";
	
	static {
		System.setProperty("log4j.configurationFile", "log4j2.xml");
	}
	
	public static void main(String[] args) {
		
		try {
			ACBClient app = new ACBClient();
			int exitCode = app.run(args);
			if (exitCode != EXIT_CODE_OK) {
				System.exit(exitCode);
			}
			
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(EXIT_CODE_FATAL);
		}
	}
	
	private int run(String[] args) {
		
		CommandLineParser parser = new DefaultParser();
		Options o = new Options();
		
		if (args.length < 2) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("ant", o);
		}
		
		args = in.split(" ");
		Path input = Paths.get(args[0]);
		
		for (String trip : new String[]{"default"/*, "salomon", "salomon+", "valach"/*, "lcp", "lengthless"*/}) {
			Path output = Paths.get(args[1] + "-" + trip);
			
			ACBFileParser fileParser = new ACBFileParser();
			ACBProvider provider = new ACBProvider(trip);
			provider.distanceBits = 6;
			provider.lengthBits = 4;
			ACB acb = new ACB(provider);
			TripletToByteConverter<?> encoder = provider.getT2BConverter();
			
			ChainBuilder.create(fileParser::open)
					.chain(acb::compress)
					.chain(encoder)
					.end(bytes -> fileParser.arrayObjectSave(bytes, output))
					.accept(input);
			
			//////////////////////////////////////////////////////
			
			ChainBuilder.create((Path path, Consumer<List<byte[]>> listConsumer) -> {
				try {
					InputStream is = Files.newInputStream(path);
					ObjectInputStream ois = new ObjectInputStream(is);
					List<byte[]> bytes = (List<byte[]>) ois.readObject();
					listConsumer.accept(bytes);
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
				}
			}).chain(provider.getB2TConverter())
					.chain(acb::decompress)
					.end(byteBuffer -> {
				try {
					byte[] inBytes = Files.readAllBytes(input);
					byte[] outBytes = byteBuffer.array();
//					logger.info("Input  = " + new String(inBytes));
//					logger.info("Output = " + new String(outBytes));
					logger.info("<{}> Input Output equals = {}", trip.toUpperCase(), Arrays.equals(inBytes, outBytes));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}).accept(output);
			
		}
		
		return EXIT_CODE_OK;
		
		// Files.newBufferedReader(path, cs);
		// Files.newInputStream(path, options);
		// Files.newByteChannel(path, options);
		// Files.readAllBytes(path);
		// Files.size(path);
		// Files.write(path, bytes, options);
		
	}
}
