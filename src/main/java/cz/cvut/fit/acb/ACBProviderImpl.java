package cz.cvut.fit.acb;

import cz.cvut.fit.acb.coding.TripletWriter;
import cz.cvut.fit.acb.dictionary.ChunkedContextIndex;
import cz.cvut.fit.acb.dictionary.ContextOrder;
import cz.cvut.fit.acb.dictionary.DecoderDictionary;
import cz.cvut.fit.acb.dictionary.EncoderDictionary;
import cz.cvut.fit.acb.dictionary.IndexedDecoderDictionary;
import cz.cvut.fit.acb.dictionary.IndexedEncoderDictionary;
import cz.cvut.fit.acb.dictionary.SegmentBuffer;
import cz.cvut.fit.acb.format.Block;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.triplets.FieldSource;

public final class ACBProviderImpl implements ACBProvider {

    private final CompressionSettings settings;

    public ACBProviderImpl(CompressionSettings settings) {
        this.settings = settings;
    }

    @Override
    public EncoderDictionary encoderDictionary(SegmentBuffer segment) {
        return new IndexedEncoderDictionary(this.indexOver(segment), segment, this.settings.maxDistance(),
                this.settings.maxLength());
    }

    @Override
    public DecoderDictionary decoderDictionary(SegmentBuffer segment) {
        return new IndexedDecoderDictionary(this.indexOver(segment));
    }

    private ChunkedContextIndex indexOver(SegmentBuffer segment) {
        return new ChunkedContextIndex(ContextOrder.byLastBytes(segment, this.settings.contextDepth()));
    }

    @Override
    public TripletWriter writer() {
        return this.settings.entropyCoding().writer(this.settings.lengthFrequencies());
    }

    @Override
    public FieldSource reader(byte[] block) throws MalformedStreamException {
        return this.settings.entropyCoding().reader(block, this.settings.lengthFrequencies());
    }

    /** The coded bytes if they are fewer than the segment's own, otherwise the segment itself. */
    @Override
    public Block block(byte[] segment, byte[] coded) {
        return coded.length < segment.length ? Block.coded(segment.length, coded) : Block.stored(segment);
    }
}
