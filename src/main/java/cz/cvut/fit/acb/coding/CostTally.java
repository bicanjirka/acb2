package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.triplets.TripletFieldKind;

import java.util.Arrays;
import java.util.List;

/** Adds up the ideal cost of the symbols a coder codes, by the kind of field they belong to. */
public final class CostTally {

    private static final double LN2 = Math.log(2);

    private final long[] symbols = new long[TripletFieldKind.values().length];
    private final double[] bits = new double[TripletFieldKind.values().length];

    /** Counts a symbol of frequency {@code frequency} in a distribution of total {@code total}. */
    public void add(TripletFieldKind kind, int total, int frequency) {
        this.symbols[kind.ordinal()]++;
        this.bits[kind.ordinal()] += Math.log((double) total / frequency) / LN2;
    }

    /** The cost of each kind that was coded, which the coded bytes exceed by the few bytes of flush. */
    public List<FieldCost> costs() {
        return Arrays.stream(TripletFieldKind.values())
                .filter(kind -> this.symbols[kind.ordinal()] > 0)
                .map(kind -> new FieldCost(kind, this.symbols[kind.ordinal()], Math.round(this.bits[kind.ordinal()])))
                .toList();
    }
}
