package cz.cvut.fit.acb.associative;

import cz.cvut.fit.acb.coding.CumulativeTable;
import cz.cvut.fit.acb.coding.RangeDecoder;
import cz.cvut.fit.acb.dictionary.Funnel;
import cz.cvut.fit.acb.dictionary.SegmentBuffer;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.triplets.TripletFieldKind;

/** The decoder's side of a step: it reads each symbol against the distribution it is given and rebuilds the text. */
final class DecodingPort implements StepPort {

    private final SegmentBuffer text;
    private final RangeDecoder decoder;

    /** @param text the segment decoded so far, empty at first */
    DecodingPort(SegmentBuffer text, RangeDecoder decoder) {
        this.text = text;
        this.decoder = decoder;
    }

    @Override
    public int position(int idx, Funnel funnel, CumulativeTable table) throws MalformedStreamException {
        return this.read(table);
    }

    @Override
    public int length(CumulativeTable table, int floor) throws MalformedStreamException {
        return this.read(table);
    }

    @Override
    public int literal(int at, CumulativeTable table) throws MalformedStreamException {
        int value = this.read(table);
        this.text.append((byte) value);
        return value;
    }

    @Override
    public void copied(int from, int count) {
        this.text.appendCopy(from, count);
    }

    @Override
    public int textByte(int at) {
        return 0;
    }

    @Override
    public int bit(TripletFieldKind kind, int encoderBit, int probabilityOfOne) throws MalformedStreamException {
        return this.decoder.decodeBit(probabilityOfOne);
    }


    @Override
    public void appended(int value) {
        this.text.append((byte) value);
    }

    private int read(CumulativeTable table) throws MalformedStreamException {
        int symbol = table.symbolAt(this.decoder.target(table.total()));
        this.decoder.consume(table.cumulative(symbol), table.frequency(symbol));
        return symbol;
    }
}
