package cz.cvut.fit.acb.coding;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import cz.cvut.fit.acb.dictionary.ByteBuilder;
import cz.cvut.fit.acb.triplets.TripletFieldId;

/**
 * @author jiri.bican
 */
public class BitArrayDecomposer extends ByteToTripletConverter<BitArrayDecomposer.ByteArrayDecomposerInner> {
	
	private ByteArrayInputStream bais;
	private BitStreamInputStream bsis;
	/** The composer pads the last byte, so the stream ends where its recorded bit count runs out. */
	private long bitsRemaining;
	
	@Override
	protected ByteArrayDecomposerInner createNew(TripletFieldId index, List<byte[]> bytes) {
		if (bais == null) {
			int byteSize = bytes.stream().mapToInt(value -> value.length).sum();
			ByteBuilder bb = new ByteBuilder(byteSize);
			for (byte[] bArr : bytes) {
				bb.append(bArr);
			}
			ByteBuffer stream = ByteBuffer.wrap(bb.array());
			bitsRemaining = stream.getLong();
			bais = new ByteArrayInputStream(bb.array(), Long.BYTES, stream.remaining());
			bsis = new BitStreamInputStream(bais);
		}
		return new ByteArrayDecomposerInner(bsis, index.bitSize());
	}
	
	@Override
	protected int decompress(ByteArrayDecomposerInner object) {
		if (bitsRemaining < object.bitSize) {
			return -1;
		}
		bitsRemaining -= object.bitSize;
		try {
			return object.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	public static class ByteArrayDecomposerInner {
		private final BitStreamInputStream inputStream;
		private final int bitSize;
		
		public ByteArrayDecomposerInner(BitStreamInputStream inputStream, int bitSize) {
			this.inputStream = inputStream;
			this.bitSize = bitSize;
		}
		
		public int read() throws IOException {
			return inputStream.read(bitSize);
		}
	}
}
