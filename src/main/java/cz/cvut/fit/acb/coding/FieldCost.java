package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.triplets.TripletFieldKind;

/** What one kind of triplet field cost: how many symbols were coded and how many bits they took. */
public record FieldCost(TripletFieldKind kind, long symbols, long bits) {

    public static FieldCost none(TripletFieldKind kind) {
        return new FieldCost(kind, 0, 0);
    }

    /** Adds two costs of the same kind; {@link #none} is the identity. */
    public FieldCost plus(FieldCost other) {
        if (other.kind != this.kind) {
            throw new IllegalArgumentException("Cannot add " + other.kind + " to " + this.kind);
        }
        return new FieldCost(this.kind, this.symbols + other.symbols, this.bits + other.bits);
    }
}
