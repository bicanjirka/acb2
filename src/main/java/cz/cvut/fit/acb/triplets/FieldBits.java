package cz.cvut.fit.acb.triplets;

/** Bit helpers for the fixed-width signed fields of a triplet. */
public final class FieldBits {

    private FieldBits() {
    }

    /** The low {@code bits} bits of {@code value}, as a field of that width stores it. */
    public static int lowBits(int value, int bits) {
        return value & ((1 << bits) - 1);
    }

    /** Undoes {@link #lowBits}: reads a {@code bits}-wide two's complement field. */
    public static int signExtend(int field, int bits) {
        int shift = Integer.SIZE - bits;
        return (field << shift) >> shift;
    }
}
