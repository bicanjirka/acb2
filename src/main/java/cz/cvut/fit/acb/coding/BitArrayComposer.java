package cz.cvut.fit.acb.coding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import cz.cvut.fit.acb.triplets.TripletFieldId;

/**
 * @author jiri.bican
 */
public class BitArrayComposer extends TripletToByteConverter<BitArrayComposer.BitArrayComposerInner> {
	
	ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
	BitStreamOutputStream bitOutputStream = new BitStreamOutputStream(arrayOutputStream);
	private boolean doReturn = false;
	private long bitsWritten;
	
	@Override
	protected byte[] getArray(BitArrayComposerInner object) {
		byte[] bytes = null;
		if (doReturn) {
			byte[] bits = arrayOutputStream.toByteArray();
			bytes = ByteBuffer.allocate(Long.BYTES + bits.length).putLong(bitsWritten).put(bits).array();
			doReturn = false;
		}
		return bytes;
	}
	
	@Override
	protected BitArrayComposerInner createNew(TripletFieldId index) {
		return new BitArrayComposerInner(bitOutputStream, index.getBitSize());
	}
	
	@Override
	protected void compress(BitArrayComposerInner object, int value) {
		try {
			object.write(value);
			bitsWritten += object.bitSize;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void terminate() {
		try {
			bitOutputStream.flush();
			doReturn = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		super.terminate();
	}
	
	public static class BitArrayComposerInner {
		private BitStreamOutputStream outputStream;
		private int bitSize;
		
		public BitArrayComposerInner(BitStreamOutputStream outputStream, int bitSize) {
			this.outputStream = outputStream;
			this.bitSize = bitSize;
		}
		
		public void write(int value) throws IOException {
			outputStream.write(value, bitSize);
		}
	}
}
