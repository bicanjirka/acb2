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

public final class LCPTripletCoder extends BaseTripletCoder {

    private final TripletFieldId distField;
    private final TripletFieldId lengField;
    private final TripletFieldId byteField;

    public LCPTripletCoder(SegmentBuffer segment, Dictionary dictionary, int distanceBits, int lengthBits) {
        super(segment, dictionary, distanceBits);
        this.distField = new TripletFieldId(0, distanceBits, TripletFieldKind.DISTANCE);
        this.lengField = new TripletFieldId(1, lengthBits, TripletFieldKind.LENGTH);
        this.byteField = new TripletFieldId(2, Byte.SIZE, TripletFieldKind.LITERAL);
    }

    @Override
    protected int encodeStep(int idx, DictionaryInfo info, Consumer<TripletSupplier> output) {
        int ctx = info.getContext();
        int cnt = info.getContent();
        int leng2 = info.getLength();
        int lcp = info.getLcp();
        int leng = leng2 + idx + lcp == segment().length() ? leng2 - 1 : leng2;

        dictionary().update(idx, leng + lcp + 1);
        idx += leng + lcp;
        int dist = cnt == -1 ? 0 : ctx - cnt;
        byte b = segment().byteAt(idx);

        output.accept(visitor -> {
            visitor.write(distField, dist & distanceMask());
            visitor.write(lengField, leng);
            visitor.write(byteField, b & 0xFF);
        });

        idx++;
        return idx;
    }

    @Override
    protected int decodeStep(int idx, TripletProcessor input) throws MalformedStreamException {
        int tempDist = input.read(distField);
        if (tempDist == -1) {
            return Integer.MAX_VALUE;
        }
        int dist = signedDistance(tempDist);
        int leng = input.read(lengField);
        int literal = input.read(byteField);
        if (leng == -1 || literal == -1) {
            throw new MalformedStreamException("The stream ends inside a triplet");
        }
        byte b = (byte) literal;

        int ctx = dictionary().searchContext(idx);
        int cnt = ctx - dist;
        int lcp = 0;

        if (leng > 0) {
            int key = dictionary().select(cnt); // find position of the best matching content in text and assume it is position of sliding window
            lcp = dictionary().searchContent(ctx, key).getLcp();
            leng += lcp;
        }

        appendContent(cnt, leng);
        segment().append(b);
        leng++;
        dictionary().update(idx, leng);
        return idx + leng;
    }
}
