package cz.cvut.fit.acb.triplets.coder;

import cz.cvut.fit.acb.dictionary.ByteBuilder;
import cz.cvut.fit.acb.dictionary.ByteSequence;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.dictionary.DictionaryInfo;
import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletProcessor;
import cz.cvut.fit.acb.triplets.TripletSupplier;
import cz.cvut.fit.acb.utils.TripletUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

/**
 * @author jiri.bican
 */
public final class SimpleTripletCoder extends BaseTripletCoder {

    private static final Logger LOG = LogManager.getLogger();

    private final TripletFieldId distField;
    private final TripletFieldId lengField;
    private final TripletFieldId byteField;

    public SimpleTripletCoder(ByteSequence sequence, Dictionary dictionary, int distanceBits, int lengthBits) {
        super(sequence, dictionary, distanceBits);
        this.distField = new TripletFieldId(0, distanceBits);
        this.lengField = new TripletFieldId(1, lengthBits, true);
        this.byteField = new TripletFieldId(2, Byte.SIZE);
    }

    @Override
    public int encodeStep(int idx, DictionaryInfo info, Consumer<TripletSupplier> output) {
        int ctx = info.getContext();
        int cnt = info.getContent();
        int leng2 = info.getLength();
        int leng = leng2 + idx == sequence().length() ? leng2 - 1 : leng2;

        dictionary().update(idx, leng + 1);
        idx += leng;
        int dist = cnt == -1 ? 0 : ctx - cnt;
        byte b = sequence().byteAt(idx);

        LOG.trace("Triplet {}", () -> TripletUtils.tripletString(dist, leng, b));
        output.accept(visitor -> {
            visitor.write(distField, dist & distanceMask());
            visitor.write(lengField, leng);
            visitor.write(byteField, b & 0xFF);
        });

        idx++;
        return idx;
    }

    @Override
    protected int decodeStep(int idx, TripletProcessor input) {
        int tempDist = input.read(distField);
        int dist = signedDistance(tempDist);
        int leng = input.read(lengField);
        int literal = input.read(byteField);
        byte b = (byte) literal;
        ByteBuilder builder = ((ByteBuilder) sequence());

        if (tempDist == -1 && leng == -1 && literal == -1) {
            return Integer.MAX_VALUE;
        }
        LOG.trace("Triplet {}", () -> TripletUtils.tripletString(dist, leng, b));

        int ctx = dictionary().searchContext(idx);
        int cnt = ctx - dist;

        if (leng > 0) {
            byte[] seq = dictionary().copy(cnt, leng);
            builder.append(seq);
        }

        builder.append(b);
        int consumed = leng + 1;
        dictionary().update(idx, consumed);
        return idx + consumed;
    }
}
