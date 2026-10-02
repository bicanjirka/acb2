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
final class EncodingPort implements StepPort {

    private final SegmentBuffer text;
    private final int maxLength;
    private final int[] alike;
    private final RangeEncoder encoder = new RangeEncoder();
    private final CostTally costs = new CostTally();
    private int matchLength;

    /**
     * @param text the whole segment
     * @param funnelCapacity the most candidates a funnel holds
     */
    EncodingPort(SegmentBuffer text, int maxLength, int funnelCapacity) {
        this.text = text;
        this.maxLength = maxLength;
        this.alike = new int[funnelCapacity];
    }

    /**
     * The first candidate with the longest match, matched against the text and never past {@code idx}. Only
     * the candidates that start with the byte at {@code idx} are matched, listed first without a branch.
     */
    @Override
    public int position(int idx, Funnel funnel, CumulativeTable table) {
        int first = Byte.toUnsignedInt(this.text.byteAt(idx));
        int alike = 0;
        for (int i = 0; i < funnel.size(); i++) {
            this.alike[alike] = i;
            alike += funnel.firstByte(i) == first ? 1 : 0;
        }
        int best = 0;
        int bestLength = 0;
        for (int k = 0; k < alike; k++) {
            int i = this.alike[k];
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

    @Override
    public int textByte(int at) {
        return Byte.toUnsignedInt(this.text.byteAt(at));
    }

    @Override
    public int bit(TripletFieldKind kind, int encoderBit, int probabilityOfOne) {
        this.encoder.encodeBit(encoderBit, probabilityOfOne);
        this.costs.add(kind, RangeEncoder.PROBABILITY_ONE,
                encoderBit != 0 ? probabilityOfOne : RangeEncoder.PROBABILITY_ONE - probabilityOfOne);
        return encoderBit;
    }


    @Override
    public void appended(int value) {
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
