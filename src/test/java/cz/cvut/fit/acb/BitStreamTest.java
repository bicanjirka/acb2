package cz.cvut.fit.acb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import cz.cvut.fit.acb.coding.BitStreamOutputStream;
import junit.framework.TestCase;

public class BitStreamTest extends TestCase {

	public BitStreamTest() {
		super("BitStream input and output tests");
	}
	
	private static void fill(int[] arr, int offset) {
		Random r = new Random();
		int bound = 1 << offset;
		for (int i = 0; i < arr.length; i++) {
			arr[i] = r.nextInt(bound);
		}
	}
	
	public void testOutputLength() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BitStreamOutputStream bit = new BitStreamOutputStream(out);
		
		byte[] in = new byte[256];
		new Random().nextBytes(in);
		bit.write(in);
		
		assertEquals(in.length, out.size());
		bit.close();
	}
	
	public void testOutputValues() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BitStreamOutputStream bit = new BitStreamOutputStream(out);
		
		byte[] in = new byte[256];
		new Random().nextBytes(in);
		bit.write(in);
		
		assertTrue(Arrays.equals(in, out.toByteArray()));
		bit.close();
	}
	
	public void testOutputLengthSameBitOffsets() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BitStreamOutputStream bit = new BitStreamOutputStream(out);
		
		int[] in = new int[256];
		for (int offset = 1; offset < 31; offset++) {
			fill(in, offset);
			for (int i : in) {
				bit.write(i, offset);
			}
			int expLeng = (int) Math.ceil(in.length / 8d * offset);
			System.out.println("offset="+offset+" length="+out.size()+" exp="+expLeng+", "+(out.size()/(float)expLeng));
//			assertEquals("offset="+offset, expLeng, out.size());
		}
		bit.close();
	}
}
