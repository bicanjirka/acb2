package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.triplets.FieldSource;
import cz.cvut.fit.acb.triplets.TripletFieldId;

/** Reads the fields {@link BitArrayWriter} wrote, from the block's bytes. */
public final class BitArrayReader implements FieldSource {

    private final byte[] bytes;
    private long position;

    public BitArrayReader(byte[] block) {
        this.bytes = block;
    }

    @Override
    public int read(TripletFieldId field) throws MalformedStreamException {
        if (this.position + field.bitSize() > (long) this.bytes.length * Byte.SIZE) {
            throw new MalformedStreamException("The stream ends inside a triplet");
        }
        int value = 0;
        int remaining = field.bitSize();
        while (remaining > 0) {
            int offset = (int) (this.position & 7);
            int take = Math.min(remaining, Byte.SIZE - offset);
            int chunk = ((this.bytes[(int) (this.position >>> 3)] & 0xFF) >>> (Byte.SIZE - offset - take))
                    & ((1 << take) - 1);
            value = (value << take) | chunk;
            this.position += take;
            remaining -= take;
        }
        return value;
    }
}
