package cz.cvut.fit.acb;

import cz.cvut.fit.acb.coding.TripletWriter;
import cz.cvut.fit.acb.dictionary.SearchWindow;
import cz.cvut.fit.acb.dictionary.SegmentBuffer;
import cz.cvut.fit.acb.format.Block;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.triplets.FieldSource;
import cz.cvut.fit.acb.triplets.SegmentDecoder;
import cz.cvut.fit.acb.triplets.SegmentEncoder;
import cz.cvut.fit.acb.triplets.TripletLayout;
import cz.cvut.fit.acb.triplets.TripletParser;

/** A segment as a run of triplets, laid out as fields and turned into bytes by the entropy coding. */
final class LayoutSegmentCoding implements SegmentCoding {

    private final TripletLayout layout;
    private final TripletParser parser;
    private final SearchWindow window;

    LayoutSegmentCoding(TripletLayout layout, TripletParser parser, SearchWindow window) {
        this.layout = layout;
        this.parser = parser;
        this.window = window;
    }

    @Override
    public CodedSegment encode(byte[] segment, ACBProvider provider) {
        SegmentBuffer buffer = SegmentBuffer.of(segment);
        TripletWriter writer = provider.writer();
        long triplets = new SegmentEncoder(provider.encoderDictionary(buffer), this.parser, this.layout, this.window)
                .encode(buffer, writer);
        Block block = provider.block(segment, writer.finish());
        return new CodedSegment(block, new CompressionStats(segment.length, 1, triplets, writer.costs()));
    }

    @Override
    public byte[] decode(Block.Coded block, ACBProvider provider) throws MalformedStreamException {
        SegmentBuffer segment = SegmentBuffer.empty();
        FieldSource source = provider.reader(block.bytes());
        new SegmentDecoder(provider.decoderDictionary(segment), this.layout, this.window)
                .decode(segment, block.rawLength(), source);
        return segment.toArray();
    }
}
