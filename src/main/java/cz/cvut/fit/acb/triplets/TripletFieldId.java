package cz.cvut.fit.acb.triplets;

/**
 * One field of a triplet: its position in the triplet, which is also the model it is
 * coded against, its width, and what it means. Length fields get the range coder's tuned
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
