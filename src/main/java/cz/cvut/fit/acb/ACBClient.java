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

import cz.cvut.fit.acb.coding.RangeCoding;

public class ACBClient {

	static String in = "corpuses/prague/gtkprint out/gtkprint";
//	static String in = "out/gtkprint out/gtkprint";

	public static void main(String[] args) {
//		try {
//			System.in.read();
//		} catch (IOException e1) {
//			e1.printStackTrace();
//		}
		args = in.split(" ");

		CommandLineParser parser = new DefaultParser();
		Options o = new Options();

		if (args.length < 2) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("ant", o);
		}
		Path input = Paths.get(args[0]);
		Path output = Paths.get(args[1]);

		for (String trip : new String[] {/* "default", "salomon", "salomon+", "valach",*/ "lcp"/*, "lengthless"*/ }) {
			try { // v lcp prohledavat tak, abych vyuzil vsechny bity length i potencial lcp, tedy kodovat stylem plna delka plny lcp, v decoderu nejdriv narvar do retezce byty z lengthu a pak se teprve dotazovat na lcp v novem slovniku, bude delsi
				byte[] b = Files.readAllBytes(input);
				b = "mississippi".getBytes();
//				b = new byte[42];
//				Arrays.fill(b, (byte) 109);
//				b[b.length - 1] = '~'; // eof
				System.out.println("\n" + trip);
				System.out.println("RAW         file size " + b.length + " ratio " + (b.length / (double) b.length));

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ACBProvider provider = new ACBProvider(4, 4, trip);
				ACB acb = new ACB(provider);
				acb.compress(b, baos);
				System.out.println("COMPRESSED  file size " + baos.size() + " ratio " + (baos.size() / (double) b.length));

				byte[] range = RangeCoding.compress(baos.toByteArray());
				System.out.println("CODED RANGE file size " + range.length + " ratio " + (range.length / (double) b.length));

//				byte[] arith = AdaptiveArithmeticCoding.compress(baos.toByteArray());
//				System.out.println("CODED ARITH file size " + arith.length + " ratio " + (arith.length / (double) b.length));

				byte[] decoded = RangeCoding.decompress(range);
				ByteArrayOutputStream baos2 = new ByteArrayOutputStream(b.length);
				acb.decompress(new ByteArrayInputStream(decoded), baos2);

				System.out.println("input output equals == " + Arrays.equals(b, baos2.toByteArray()));
				System.out.println(new String(b));
				/*if (ACB.print_dict)*/ System.out.println(new String(baos2.toByteArray()));
//				 Files.deleteIfExists(output);
//				 Files.write(output, baos.toByteArray(), StandardOpenOption.CREATE);

			} catch (IOException e) {
				e.printStackTrace();
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
