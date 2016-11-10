package cz.cvut.fit.acb;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import cz.cvut.fit.acb.coding.TripletToByteConverter;
import cz.cvut.fit.acb.utils.ChainAdapter;
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
	
	private static final Logger logger = LogManager.getLogger();
	static String in = "corpuses/mailflder corpuses/out";
	
	static {
		System.setProperty("log4j.configurationFile", "log4j2.xml");
	}
	
	public static void main(String[] args) {
//		try {
//			System.in.read();
//		} catch (IOException e1) {
//			e1.printStackTrace();
//		}
		args = in.split(" ");
//		System.setProperty("org.apache.logging.log4j.simplelog.StatusLogger.level", "info");
		
		CommandLineParser parser = new DefaultParser();
		Options o = new Options();
		
		if (args.length < 2) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("ant", o);
		}
		Path input = Paths.get(args[0]);
		
		for (String trip : new String[]{"default", "salomon", "salomon+", "valach"/*, "lcp", "lengthless"*/}) {
			Path output = Paths.get(args[1] + "-" + trip);
			
			ACBFileParser fileParser = new ACBFileParser();
			ACBProvider provider = new ACBProvider(6, 4, trip);
			ACB acb = new ACB(provider);
			TripletToByteConverter<?> encoder = provider.getT2BConverter();
			
			ChainBuilder.create(fileParser).chain(acb.compress()).chain(encoder).end(bytes -> {
				try {
					Files.deleteIfExists(output);
					OutputStream os = Files.newOutputStream(output);
					ObjectOutputStream oos = new ObjectOutputStream(os);
					oos.writeObject(bytes);
					oos.close();
					
					int byteSize = bytes.stream().mapToInt(value -> value.length).sum();
					long finalSize = Files.size(output);
					logger.info("Compressed into '{}' [size = {}, bytes = {}]", output, finalSize, byteSize);
					StringBuilder sb = new StringBuilder("Outputted byte array:");
					for (int i = 0; i < bytes.size(); i++) {
						byte[] b = bytes.get(i);
						sb.append("\n").append(i).append(": ").append(b.length);
					}
					logger.info(sb.toString());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}).accept(input);
			
			//////////////////////////////////////////////////////
			
			ChainBuilder.create(new ChainAdapter<Path, List<byte[]>>((path, listConsumer) -> {
				try {
					InputStream is = Files.newInputStream(path);
					ObjectInputStream ois = new ObjectInputStream(is);
					List<byte[]> bytes = (List<byte[]>) ois.readObject();
					listConsumer.accept(bytes);
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
				}
			})).chain(provider.getB2TConverter()).chain(acb.decompress()).end(byteBuffer -> {
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
		
		// Files.newBufferedReader(path, cs);
		// Files.newInputStream(path, options);
		// Files.newByteChannel(path, options);
		// Files.readAllBytes(path);
		// Files.size(path);
		// Files.write(path, bytes, options);
		
	}
}
