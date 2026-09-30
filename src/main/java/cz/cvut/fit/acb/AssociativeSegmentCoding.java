package cz.cvut.fit.acb;

import cz.cvut.fit.acb.associative.AssociativeDecoder;
import cz.cvut.fit.acb.associative.AssociativeEncoder;
import cz.cvut.fit.acb.associative.AssociativeVariant;
import cz.cvut.fit.acb.dictionary.SegmentBuffer;
import cz.cvut.fit.acb.format.Block;
import cz.cvut.fit.acb.format.MalformedStreamException;

/** A segment coded by the funnels of analogies of its steps. */
final class AssociativeSegmentCoding implements SegmentCoding {

    private final AssociativeVariant variant;
    private final int reach;
    private final int maxLength;
    private final int[] lengthStart;

    AssociativeSegmentCoding(AssociativeVariant variant, CompressionSettings settings) {
        this.variant = variant;
        this.reach = settings.maxDistance();
        this.maxLength = settings.maxLength();
        this.lengthStart = settings.lengthFrequencies()
                .startingTable(CompressionSettings.lengthAlphabetSize(settings.lengthBits()));
    }

    @Override
    public CodedSegment encode(byte[] segment, ACBProvider provider) {
        SegmentBuffer buffer = SegmentBuffer.of(segment);
        AssociativeEncoder encoder = new AssociativeEncoder(this.variant, provider.analogyDictionary(buffer), buffer, this.reach,
                this.maxLength, this.lengthStart);
        long steps = encoder.encode();
        Block block = provider.block(segment, encoder.finish());
        return new CodedSegment(block, new CompressionStats(segment.length, 1, steps, encoder.costs()));
    }

    @Override
    public byte[] decode(Block.Coded block, ACBProvider provider) throws MalformedStreamException {
        SegmentBuffer segment = SegmentBuffer.empty();
        new AssociativeDecoder(this.variant, provider.analogyDictionary(segment), segment, this.reach, this.maxLength,
                this.lengthStart).decode(block.bytes(), block.rawLength());
        return segment.toArray();
    }
}
