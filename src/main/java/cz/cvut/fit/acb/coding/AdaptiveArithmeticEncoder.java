package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.triplets.TripletFieldId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AdaptiveArithmeticEncoder extends TripletToByteConverter<AdaptiveArithmeticCompress> {

    private final Collection<Runnable> onTerminate = new ArrayList<>();
    private final int[] lengthFreq;

    public AdaptiveArithmeticEncoder(int[] lengthFreq) {
        this.lengthFreq = lengthFreq.clone();
    }

    @Override
    protected byte[] getArray(AdaptiveArithmeticCompress object) {
        return object.array();
    }

    @Override
    protected AdaptiveArithmeticCompress createNew(TripletFieldId fieldId) {
        AdaptiveArithmeticCompress inst = fieldId.isLength() ?
                new AdaptiveArithmeticCompress(fieldId.bitSize(), lengthFreq) :
                new AdaptiveArithmeticCompress(fieldId.bitSize());
        onTerminate.add(inst::terminate);
        return inst;
    }

    @Override
    protected void compress(AdaptiveArithmeticCompress object, int value) {
        object.compress(value);
    }

    @Override
    public List<byte[]> finish() {
        onTerminate.forEach(Runnable::run);
        return super.finish();
    }
}
