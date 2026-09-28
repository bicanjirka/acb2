package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.triplets.TripletFieldId;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.List;

public class BitArrayComposer extends TripletToByteConverter<BitArrayComposer.BitArrayComposerInner> {

    private final ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
    private final BitStreamOutputStream bitOutputStream = new BitStreamOutputStream(arrayOutputStream);
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
        return new BitArrayComposerInner(bitOutputStream, index.bitSize());
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
    public List<byte[]> finish() {
        try {
            bitOutputStream.flush();
            doReturn = true;
        } catch (IOException e) {
            throw new UncheckedIOException("In-memory flush failed", e);
        }
        return super.finish();
    }

    public static class BitArrayComposerInner {
        private final BitStreamOutputStream outputStream;
        private final int bitSize;

        public BitArrayComposerInner(BitStreamOutputStream outputStream, int bitSize) {
            this.outputStream = outputStream;
            this.bitSize = bitSize;
        }

        public void write(int value) throws IOException {
            outputStream.write(value, bitSize);
        }
    }
}
