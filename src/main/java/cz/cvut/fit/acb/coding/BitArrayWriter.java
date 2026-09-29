package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletFieldKind;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Writes every field of a block at its full width, most significant bit first, into one bit stream,
 * with no modelling. Its only use is to measure what the range coder gains.
 */
public final class BitArrayWriter implements TripletWriter {

    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private final long[] symbols = new long[TripletFieldKind.values().length];
    private final long[] bits = new long[TripletFieldKind.values().length];
    private int partial;
    private int partialBits;

    @Override
    public void write(TripletFieldId field, int value) {
        int kind = field.kind().ordinal();
        this.symbols[kind]++;
        this.bits[kind] += field.bitSize();
        for (int bit = field.bitSize() - 1; bit >= 0; bit--) {
            this.partial = (this.partial << 1) | ((value >>> bit) & 1);
            if (++this.partialBits == Byte.SIZE) {
                this.bytes.write(this.partial);
                this.partial = 0;
                this.partialBits = 0;
            }
        }
    }

    /** The last byte is padded with zero bits. */
    @Override
    public byte[] finish() {
        if (this.partialBits > 0) {
            this.bytes.write(this.partial << (Byte.SIZE - this.partialBits));
            this.partial = 0;
            this.partialBits = 0;
        }
        return this.bytes.toByteArray();
    }

    @Override
    public List<FieldCost> costs() {
        return Arrays.stream(TripletFieldKind.values())
                .filter(kind -> this.symbols[kind.ordinal()] > 0)
                .map(kind -> new FieldCost(kind, this.symbols[kind.ordinal()], this.bits[kind.ordinal()]))
                .toList();
    }
}
