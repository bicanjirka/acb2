package cz.cvut.fit.acb.coding;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import nayuki.arithcode.AdaptiveArithmeticCompress;
import nayuki.arithcode.AdaptiveArithmeticDecompress;
import nayuki.arithcode.BitInputStream;
import nayuki.arithcode.BitOutputStream;

public class AdaptiveArithmeticCoding {
	
	public static byte[] compress(byte[] b) {
		try {
			InputStream in = new ByteArrayInputStream(b);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			BitOutputStream bitOut = new BitOutputStream(out);
			AdaptiveArithmeticCompress.compress(in, bitOut);
			bitOut.close();
			return out.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static byte[] decompress(byte[] b) {
		try {
			InputStream in = new ByteArrayInputStream(b);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			AdaptiveArithmeticDecompress.decompress(new BitInputStream(in), out);
			return out.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
