package cz.cvut.fit.acb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import cz.cvut.fit.acb.coding.AdaptiveArithmeticCoding;
import cz.cvut.fit.acb.coding.RangeCoding;

public class ACBClient {

	static String in = "corpuses/mailflder out/out.txt";
	// static String in = "corpuses/xmlevent out/out.txt";

	public static void main(String[] args) {
		args = in.split(" ");

		CommandLineParser parser = new DefaultParser();
		Options o = new Options();

		if (args.length < 2) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("ant", o);
		}
		Path input = Paths.get(args[0]);
		Path output = Paths.get(args[1]);

		for (String trip : new String[] { "default", "salomon", "salomon+", "valach" }) {
			try {
				byte[] b = Files.readAllBytes(input);
				// b = "mississippi".getBytes();
				System.out.println("\n" + trip);
				System.out.println("RAW         file size " + b.length + " ratio " + (b.length / (double) b.length));

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ACB acb = new ACB(32, 128, trip);
				acb.compress(b, baos);
				System.out.println("COMPRESSED  file size " + baos.size() + " ratio " + (baos.size() / (double) b.length));

				byte[] range = RangeCoding.compress(baos.toByteArray());
				System.out.println("CODED RANGE file size " + range.length + " ratio " + (range.length / (double) b.length));

				byte[] arith = AdaptiveArithmeticCoding.compress(baos.toByteArray());
				System.out.println("CODED ARITH file size " + arith.length + " ratio " + (arith.length / (double) b.length));

				byte[] decoded = RangeCoding.decompress(range);
				ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
				acb.decompress(new ByteArrayInputStream(decoded), baos2);

				System.out.println("input output equals == " + Arrays.equals(b, baos2.toByteArray()));
				// Files.deleteIfExists(output);
				// Files.write(output, baos2.toByteArray(), StandardOpenOption.CREATE);

			} catch (IOException e) {
				// TODO: handle exception
			}
		}

		// Files.newBufferedReader(path, cs);
		// Files.newInputStream(path, options);
		// Files.newByteChannel(path, options);
		// Files.readAllBytes(path);
		// Files.size(path);
		// Files.write(path, bytes, options);

	}
}
