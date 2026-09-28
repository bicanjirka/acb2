package cz.cvut.fit.acb.triplets;

/**
 * One field of a triplet: its position in the triplet, which is also the payload stream it is
 * coded into, its width, and what it means. Length fields get the arithmetic coder's tuned
 * starting table.
 */
public record TripletFieldId(int index, int bitSize, TripletFieldKind kind) {

    public boolean isLength() {
        return this.kind == TripletFieldKind.LENGTH;
    }

    @Override
    public String toString() {
        return "[" + this.index + ", " + this.bitSize + ", " + this.kind + ']';
    }
}
