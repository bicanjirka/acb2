package cz.cvut.fit.acb.triplets.coder;

import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.dictionary.DictionaryInfo;
import cz.cvut.fit.acb.dictionary.SegmentBuffer;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletFieldKind;
import cz.cvut.fit.acb.triplets.TripletProcessor;
import cz.cvut.fit.acb.triplets.TripletSupplier;

import java.util.function.Consumer;

public final class ValachTripletCoder extends BaseTripletCoder {

    private final TripletFieldId distField;
    private final TripletFieldId lengField;
    private final TripletFieldId byteField;

    public ValachTripletCoder(SegmentBuffer segment, Dictionary dictionary, int distanceBits, int lengthBits) {
        super(segment, dictionary, distanceBits);
        this.lengField = new TripletFieldId(0, lengthBits, TripletFieldKind.LENGTH);
        this.distField = new TripletFieldId(1, distanceBits, TripletFieldKind.DISTANCE);
        this.byteField = new TripletFieldId(2, Byte.SIZE, TripletFieldKind.LITERAL);
    }

    @Override
    protected int encodeStep(int idx, DictionaryInfo info, Consumer<TripletSupplier> output) {
        int ctx = info.getContext();
        int cnt = info.getContent();
        int leng = info.getLength();
        // A match reaching the end of the segment leaves no literal, so it gives up its last byte;
        // shortened to zero it must be written as a literal, which is all the decoder expects.
        int leng2 = leng + idx == segment().length() ? leng - 1 : leng;

        int dist = cnt == -1 ? 0 : ctx - cnt;
        if (leng2 == 0) {
            dictionary().update(idx, 1);
            byte b = segment().byteAt(idx);
            output.accept(visitor -> {
                visitor.write(lengField, 0);
                visitor.write(byteField, b & 0xFF);
            });
        } else {
            dictionary().update(idx, leng2 + 1);
            idx += leng2;
            byte b = segment().byteAt(idx);
            output.accept(visitor -> {
                visitor.write(lengField, leng2);
                visitor.write(distField, dist & distanceMask());
                visitor.write(byteField, b & 0xFF);
            });
        }
        return idx + 1;
    }

    @Override
    protected int decodeStep(int idx, TripletProcessor input) throws MalformedStreamException {
        int leng = input.read(lengField);
        if (leng == -1) {
            return Integer.MAX_VALUE;
        }
        if (leng == 0) {
            byte b = (byte) requireField(input.read(byteField));
            segment().append(b);
            dictionary().update(idx, 1);
            return idx + 1;
        } else {
            int tempDist = requireField(input.read(distField));
            int dist = signedDistance(tempDist);
            byte b = (byte) requireField(input.read(byteField));

            int ctx = dictionary().searchContext(idx);
            int cnt = ctx - dist;
            appendContent(cnt, leng);
            segment().append(b);
            int consumed = leng + 1;
            dictionary().update(idx, consumed);
            return idx + consumed;
        }
    }

}
