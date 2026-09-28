package cz.cvut.fit.acb.triplets;

/**
 * One field of a triplet: its position in the triplet, which is also the payload stream it is
 * coded into, and its width. Length fields get the arithmetic coder's tuned starting table.
 *
 * @author jiri.bican
 */
public record TripletFieldId(int index, int bitSize, boolean isLength) {

    public TripletFieldId(int index, int bitSize) {
        this(index, bitSize, false);
    }

    @Override
    public String toString() {
        return "[" + this.index + ", " + this.bitSize + ']';
    }
}
