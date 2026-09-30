package cz.cvut.fit.acb.associative;

import cz.cvut.fit.acb.coding.CostTally;
import cz.cvut.fit.acb.coding.CumulativeTable;
import cz.cvut.fit.acb.coding.FieldCost;
import cz.cvut.fit.acb.coding.RangeEncoder;
import cz.cvut.fit.acb.dictionary.Funnel;
import cz.cvut.fit.acb.dictionary.SegmentBuffer;
import cz.cvut.fit.acb.triplets.TripletFieldKind;

import java.util.List;

/**
 * The encoder's side of a step: it finds in the text which candidate continues it, and range-codes
 * that and what follows against the distributions it is given.
 */
final class EncodingPort implements SymbolPort {

    private final SegmentBuffer text;
    private final int maxLength;
    private final RangeEncoder encoder = new RangeEncoder();
    private final CostTally costs = new CostTally();
    private int matchLength;

    /** @param text the whole segment */
    EncodingPort(SegmentBuffer text, int maxLength) {
        this.text = text;
        this.maxLength = maxLength;
    }

    /** The first candidate with the longest match, matched against the text and never past {@code idx}. */
    @Override
    public int position(int idx, Funnel funnel, CumulativeTable table) {
        int best = 0;
        int bestLength = 0;
        for (int i = 0; i < funnel.size(); i++) {
            int content = funnel.position(i);
            int length = this.text.commonLength(idx, content, Math.min(this.maxLength, idx - content));
            if (length > bestLength) {
                bestLength = length;
                best = i + 1;
            }
        }
        this.matchLength = bestLength;
        this.code(TripletFieldKind.DISTANCE, table, best);
        return best;
    }

    @Override
    public int length(CumulativeTable table, int floor) {
        int excess = this.matchLength - floor - 1;
        this.code(TripletFieldKind.LENGTH, table, excess);
        return excess;
    }

    @Override
    public int literal(int at, CumulativeTable table) {
        int value = Byte.toUnsignedInt(this.text.byteAt(at));
        this.code(TripletFieldKind.LITERAL, table, value);
        return value;
    }

    @Override
    public void copied(int from, int count) {
    }

    byte[] finish() {
        this.encoder.finish();
        return this.encoder.toArray();
    }

    List<FieldCost> costs() {
        return this.costs.costs();
    }

    private void code(TripletFieldKind kind, CumulativeTable table, int symbol) {
        int frequency = table.frequency(symbol);
        this.encoder.encode(table.cumulative(symbol), frequency, table.total());
        this.costs.add(kind, table.total(), frequency);
    }
}
