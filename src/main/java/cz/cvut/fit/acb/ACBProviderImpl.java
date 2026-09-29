package cz.cvut.fit.acb;

import cz.cvut.fit.acb.coding.AdaptiveArithmeticDecoder;
import cz.cvut.fit.acb.coding.AdaptiveArithmeticEncoder;
import cz.cvut.fit.acb.coding.BitArrayComposer;
import cz.cvut.fit.acb.coding.BitArrayDecomposer;
import cz.cvut.fit.acb.coding.ByteToTripletConverter;
import cz.cvut.fit.acb.coding.TripletWriter;
import cz.cvut.fit.acb.dictionary.ChunkedContextIndex;
import cz.cvut.fit.acb.dictionary.ContextOrder;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.dictionary.DictionaryBase;
import cz.cvut.fit.acb.dictionary.SegmentBuffer;
import cz.cvut.fit.acb.triplets.TripletProcessor;
import cz.cvut.fit.acb.triplets.coder.SalomonTripletCoder;
import cz.cvut.fit.acb.triplets.coder.SimpleTripletCoder;
import cz.cvut.fit.acb.triplets.coder.TripletCoder;
import cz.cvut.fit.acb.triplets.coder.ValachTripletCoder;

import java.util.List;

public final class ACBProviderImpl implements ACBProvider {

    private final CompressionSettings settings;

    public ACBProviderImpl(CompressionSettings settings) {
        this.settings = settings;
    }

    @Override
    public Dictionary getDictionary(SegmentBuffer segment) {
        int maxDistance = this.settings.maxDistance();
        int maxLength = this.settings.maxLength();
        ChunkedContextIndex index = new ChunkedContextIndex(ContextOrder.byLastBytes(segment));
        return new DictionaryBase(index, segment, maxDistance, maxLength);
    }

    @Override
    public TripletCoder getCoder(SegmentBuffer segment, Dictionary dictionary) {
        int distanceBits = this.settings.distanceBits();
        int lengthBits = this.settings.lengthBits();
        return switch (this.settings.tripletCoding()) {
            case SALOMON -> new SalomonTripletCoder.SalomonByteless(segment, dictionary, distanceBits, lengthBits);
            case SALOMON2 -> new SalomonTripletCoder.SalomonByteful(segment, dictionary, distanceBits, lengthBits);
            case SIMPLE -> new SimpleTripletCoder(segment, dictionary, distanceBits, lengthBits);
            case VALACH -> new ValachTripletCoder(segment, dictionary, distanceBits, lengthBits);
        };
    }

    @Override
    public TripletWriter getTripletWriter() {
        return switch (this.settings.entropyCoding()) {
            case ADAPTIVE_ARITHMETIC -> new AdaptiveArithmeticEncoder(this.settings.lengthFrequencies());
            case BIT_ARRAY -> new BitArrayComposer();
        };
    }

    @Override
    public TripletProcessor getTripletReader(List<byte[]> payload) {
        ByteToTripletConverter<?> converter = switch (this.settings.entropyCoding()) {
            case ADAPTIVE_ARITHMETIC -> new AdaptiveArithmeticDecoder(this.settings.lengthFrequencies());
            case BIT_ARRAY -> new BitArrayDecomposer();
        };
        return converter.open(payload);
    }
}
