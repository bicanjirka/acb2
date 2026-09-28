package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.triplets.TripletFieldId;

import java.util.List;

public class AdaptiveArithmeticDecoder extends ByteToTripletConverter<AdaptiveArithmeticDecompress> {

    private final int[] lengthFreq;

    public AdaptiveArithmeticDecoder(int[] lengthFreq) {
        this.lengthFreq = lengthFreq.clone();
    }

    @Override
    protected AdaptiveArithmeticDecompress createNew(TripletFieldId index, List<byte[]> bytes)
            throws MalformedStreamException {
        if (index.index() >= bytes.size()) {
            throw new MalformedStreamException("The payload has no array for triplet field " + index.index());
        }
        return index.isLength() ?
                new AdaptiveArithmeticDecompress(index.bitSize(), bytes.get(index.index()), lengthFreq) :
                new AdaptiveArithmeticDecompress(index.bitSize(), bytes.get(index.index()));
    }

    @Override
    protected int decompress(AdaptiveArithmeticDecompress object) throws MalformedStreamException {
        return object.decompress();
    }
}
