package cz.cvut.fit.acb;

import cz.cvut.fit.acb.coding.TripletToByteConverter;
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
	private static final int EXIT_CODE_FATAL = 1;
	private static final Logger logger = LogManager.getLogger();
	
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
		
		ACBProviderParameters params = new ACBProviderParameters();
		ACBFileIO io = new ACBFileIO();
		ACBProviderImpl provider = new ACBProviderImpl(params);
		ACB acb = new ACB(provider);
		TripletToByteConverter<?> encoder = provider.getT2BConverter();
		
		/*ChainBuilder.create(io::openParse)
				.chain(acb::compress)
				.chain(encoder)
				.end(bytes -> io.saveObject(bytes, output))
				.accept(input);
		
		//////////////////////////////////////////////////////
		
		ChainBuilder.create(io::openObject)
				.chain(provider.getB2TConverter())
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
		*/
		
		return EXIT_CODE_OK;
	}
}
