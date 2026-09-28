package cz.cvut.fit.acb.triplets.coder;

import cz.cvut.fit.acb.dictionary.ByteBuilder;
import cz.cvut.fit.acb.dictionary.ByteSequence;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.dictionary.DictionaryInfo;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletFieldKind;
import cz.cvut.fit.acb.triplets.TripletProcessor;
import cz.cvut.fit.acb.triplets.TripletSupplier;
import cz.cvut.fit.acb.utils.TripletUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

public final class ValachTripletCoder extends BaseTripletCoder {

    private static final Logger LOG = LogManager.getLogger();

    private final TripletFieldId distField;
    private final TripletFieldId lengField;
    private final TripletFieldId byteField;

    public ValachTripletCoder(ByteSequence sequence, Dictionary dictionary, int distanceBits, int lengthBits) {
        super(sequence, dictionary, distanceBits);
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
        int leng2 = leng + idx == sequence().length() ? leng - 1 : leng;

        int dist = cnt == -1 ? 0 : ctx - cnt;
        if (leng2 == 0) {
            dictionary().update(idx, 1);
            byte b = sequence().byteAt(idx);
            LOG.trace("Triplet {}", () -> TripletUtils.tripletString(0, b));
            output.accept(visitor -> {
                visitor.write(lengField, 0);
                visitor.write(byteField, b & 0xFF);
            });
        } else {
            dictionary().update(idx, leng2 + 1);
            idx += leng2;
            byte b = sequence().byteAt(idx);
            LOG.trace("Triplet {}", () -> TripletUtils.tripletString(leng2, dist, b));
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
        ByteBuilder builder = ((ByteBuilder) sequence());
        int leng = input.read(lengField);
        if (leng == -1) {
            return Integer.MAX_VALUE;
        }
        if (leng == 0) {
            byte b = (byte) requireField(input.read(byteField));
            LOG.trace("Triplet {}", () -> TripletUtils.tripletString(0, b));
            builder.append(b);
            dictionary().update(idx, 1);
            return idx + 1;
        } else {
            int tempDist = requireField(input.read(distField));
            int dist = signedDistance(tempDist);
            byte b = (byte) requireField(input.read(byteField));
            LOG.trace("Triplet {}", () -> TripletUtils.tripletString(leng, dist, b));

            int ctx = dictionary().searchContext(idx);
            int cnt = ctx - dist;
            byte[] seq = dictionary().copy(cnt, leng);
            builder.append(seq).append(b);
            int consumed = leng + 1;
            dictionary().update(idx, consumed);
            return idx + consumed;
        }
    }

}
