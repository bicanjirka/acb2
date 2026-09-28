package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.dictionary.ByteBuilder;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.triplets.TripletFieldId;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class BitArrayDecomposer extends ByteToTripletConverter<BitArrayDecomposer.ByteArrayDecomposerInner> {

    private ByteArrayInputStream bais;
    private BitStreamInputStream bsis;
    /** The composer pads the last byte, so the stream ends where its recorded bit count runs out. */
    private long bitsRemaining;

    @Override
    protected ByteArrayDecomposerInner createNew(TripletFieldId index, List<byte[]> bytes)
            throws MalformedStreamException {
        if (bais == null) {
            int byteSize = bytes.stream().mapToInt(value -> value.length).sum();
            if (byteSize < Long.BYTES) {
                throw new MalformedStreamException("The bit stream has no room for its bit count");
            }
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
    protected int decompress(ByteArrayDecomposerInner object) throws MalformedStreamException {
        if (bitsRemaining < object.bitSize) {
            return -1;
        }
        bitsRemaining -= object.bitSize;
        try {
            return object.read();
        } catch (IOException e) {
            throw new MalformedStreamException("The bit stream cannot be read: " + e.getMessage());
        }
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
