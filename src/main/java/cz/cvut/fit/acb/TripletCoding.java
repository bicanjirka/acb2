package cz.cvut.fit.acb;

import cz.cvut.fit.acb.triplets.TripletLayout;
import cz.cvut.fit.acb.triplets.TripletParser;
import cz.cvut.fit.acb.triplets.coder.BareMatchParser;
import cz.cvut.fit.acb.triplets.coder.LiteralAfterMatchParser;
import cz.cvut.fit.acb.triplets.coder.SalomonTripletLayout;
import cz.cvut.fit.acb.triplets.coder.SimpleTripletLayout;
import cz.cvut.fit.acb.triplets.coder.ValachTripletLayout;

import java.util.Arrays;
import java.util.Optional;

/**
 * How a match is laid out as triplet fields. A constant carries all there is to a coder: its code in
 * the container format, the layout of its fields and the parser that turns matches into triplets, so
 * a new coder is added here and nowhere else. Codes are fixed here rather than taken from ordinals,
 * so reordering the constants cannot change what old files mean.
 */
public enum TripletCoding {

    SIMPLE(0, SimpleTripletLayout::new, new LiteralAfterMatchParser()),
    SALOMON(1, SalomonTripletLayout::bare, new BareMatchParser()),
    SALOMON2(2, SalomonTripletLayout::withLiteral, new LiteralAfterMatchParser()),
    VALACH(3, ValachTripletLayout::new, new LiteralAfterMatchParser());

    /** Makes the layout of a coder for the given field widths. */
    @FunctionalInterface
    private interface LayoutFactory {
        TripletLayout create(int distanceBits, int lengthBits);
    }

    private final int formatCode;
    private final LayoutFactory layouts;
    private final TripletParser parser;

    TripletCoding(int formatCode, LayoutFactory layouts, TripletParser parser) {
        this.formatCode = formatCode;
        this.layouts = layouts;
        this.parser = parser;
    }

    /** The code the container format stores for this coder. */
    public int formatCode() {
        return this.formatCode;
    }

    public static Optional<TripletCoding> byFormatCode(int code) {
        return Arrays.stream(values()).filter(coding -> coding.formatCode == code).findFirst();
    }

    public TripletLayout layout(int distanceBits, int lengthBits) {
        return this.layouts.create(distanceBits, lengthBits);
    }

    /** The parser of the encoder; it holds no state, so one serves every stream. */
    public TripletParser parser() {
        return this.parser;
    }
}
