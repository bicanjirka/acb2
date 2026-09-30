package cz.cvut.fit.acb;

import cz.cvut.fit.acb.dictionary.MatchRule;
import cz.cvut.fit.acb.dictionary.SearchWindow;
import cz.cvut.fit.acb.triplets.TripletLayout;
import cz.cvut.fit.acb.triplets.TripletParser;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * A coder that searches a window of ranks for the best match by a {@link MatchRule}, parses it into
 * triplets and lays them out as fields, which an entropy coding turns into bytes.
 */
public final class LayoutCoder implements Coder {

    /** Makes the layout of a coder for the given field widths. */
    @FunctionalInterface
    interface LayoutFactory {
        TripletLayout create(int distanceBits, int lengthBits);
    }

    private final LayoutFactory layouts;
    private final TripletParser parser;
    private final MatchRule matchRule;

    private LayoutCoder(LayoutFactory layouts, TripletParser parser, MatchRule matchRule) {
        this.layouts = layouts;
        this.parser = parser;
        this.matchRule = matchRule;
    }

    static LayoutCoder of(LayoutFactory layouts, TripletParser parser, MatchRule matchRule) {
        return new LayoutCoder(layouts, parser, matchRule);
    }

    public TripletLayout layout(int distanceBits, int lengthBits) {
        return this.layouts.create(distanceBits, lengthBits);
    }

    /** What the dictionaries and the literal contexts need of the settings. */
    public SearchWindow window(CompressionSettings settings) {
        return new SearchWindow(settings.contextDepth(), settings.maxDistance(), settings.maxLength(), this.matchRule);
    }

    @Override
    public SegmentCoding segmentCoding(CompressionSettings settings) {
        return new LayoutSegmentCoding(this.layout(settings.distanceBits(), settings.lengthBits()), this.parser,
                this.window(settings));
    }

    @Override
    public Optional<LayoutCoder> layoutCoder() {
        return Optional.of(this);
    }

    @Override
    public Set<EntropyCoding> entropyCodings() {
        return EnumSet.allOf(EntropyCoding.class);
    }

    @Override
    public CompressionSettings preset(CompressionSettings defaults) {
        return defaults;
    }
}
