package cz.cvut.fit.acb.coding;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import cz.cvut.fit.acb.dictionary.ByteBuilder;
import cz.cvut.fit.acb.triplets.TripletFieldId;

/**
 * @author jiri.bican
 */
public class BitArrayDecomposer extends ByteToTripletConverter<BitArrayDecomposer.ByteArrayDecomposerInner> {
	
	ByteArrayInputStream bais;
	BitStreamInputStream bsis;
	
	@Override
	protected ByteArrayDecomposerInner createNew(TripletFieldId index, List<byte[]> bytes) {
		if (bais == null) {
			int byteSize = bytes.stream().mapToInt(value -> value.length).sum();
			ByteBuilder bb = new ByteBuilder(byteSize);
			for (byte[] bArr : bytes) {
				bb.append(bArr);
			}
			bais = new ByteArrayInputStream(bb.array());
			bsis = new BitStreamInputStream(bais);
		}
		return new ByteArrayDecomposerInner(bsis, index.getBitSize());
	}
	
	@Override
	protected int decompress(ByteArrayDecomposerInner object) {
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
