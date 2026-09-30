package cz.cvut.fit.acb;

import cz.cvut.fit.acb.dictionary.MatchRule;
import cz.cvut.fit.acb.triplets.coder.BareMatchParser;
import cz.cvut.fit.acb.triplets.coder.LiteralAfterMatchParser;
import cz.cvut.fit.acb.triplets.coder.SalomonTripletLayout;
import cz.cvut.fit.acb.triplets.coder.SimpleTripletLayout;
import cz.cvut.fit.acb.triplets.coder.ValachTripletLayout;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/**
 * How a segment is coded. A constant carries all there is to a coder: its code in the container
 * format and what codes a segment, so a new coder is added here and nowhere else. Codes are fixed
 * here rather than taken from ordinals, so reordering the constants cannot change what old files mean.
 */
public enum TripletCoding {

    SIMPLE(0, LayoutCoder.of(SimpleTripletLayout::new, new LiteralAfterMatchParser(), MatchRule.NEAREST)),
    SALOMON(1, LayoutCoder.of(SalomonTripletLayout::bare, new BareMatchParser(), MatchRule.NEAREST)),
    SALOMON2(2, LayoutCoder.of(SalomonTripletLayout::withLiteral, new LiteralAfterMatchParser(),
            MatchRule.NEAREST)),
    VALACH(3, LayoutCoder.of(ValachTripletLayout::new, new LiteralAfterMatchParser(), MatchRule.NEAREST)),
    LCP(4, LayoutCoder.of(SimpleTripletLayout::new, new LiteralAfterMatchParser(), MatchRule.SMALLEST_WITH_LCP)),
    ACB(5, new AssociativeCoder()),
    PREFIX(6, LayoutCoder.of(ValachTripletLayout::new, new LiteralAfterMatchParser(),
            MatchRule.NEAREST_WITH_PREFIX));

    private final int formatCode;
    private final Coder coder;

    TripletCoding(int formatCode, Coder coder) {
        this.formatCode = formatCode;
        this.coder = coder;
    }

    /** The code the container format stores for this coder. */
    public int formatCode() {
        return this.formatCode;
    }

    public static Optional<TripletCoding> byFormatCode(int code) {
        return Arrays.stream(values()).filter(coding -> coding.formatCode == code).findFirst();
    }

    /** What codes a segment, for the settings of a stream; it holds no state, so it serves every segment. */
    public SegmentCoding segmentCoding(CompressionSettings settings) {
        return this.coder.segmentCoding(settings);
    }

    /** The coder as a layout of triplet fields over a window of ranks; empty for a coder that is not one. */
    public Optional<LayoutCoder> layoutCoder() {
        return this.coder.layoutCoder();
    }

    /** The entropy codings this coder can be used with. */
    public Set<EntropyCoding> entropyCodings() {
        return this.coder.entropyCodings();
    }

    /** The settings this coder starts from, given the general defaults with this coder chosen. */
    CompressionSettings preset(CompressionSettings defaults) {
        return this.coder.preset(defaults);
    }
}
