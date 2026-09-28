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

import java.util.function.Consumer;

public abstract sealed class SalomonTripletCoder extends BaseTripletCoder
        permits SalomonTripletCoder.SalomonByteless, SalomonTripletCoder.SalomonByteful {

    private static final int BIT_FLAG = 1;

    private final TripletFieldId flagField;
    private final TripletFieldId distField;
    private final TripletFieldId lengField;
    private final TripletFieldId byteField;

    protected SalomonTripletCoder(ByteSequence sequence, Dictionary dictionary, int distanceBits, int lengthBits) {
        super(sequence, dictionary, distanceBits);
        this.flagField = new TripletFieldId(0, BIT_FLAG, TripletFieldKind.FLAG);
        this.distField = new TripletFieldId(1, distanceBits, TripletFieldKind.DISTANCE);
        this.lengField = new TripletFieldId(2, lengthBits, TripletFieldKind.LENGTH);
        this.byteField = new TripletFieldId(3, Byte.SIZE, TripletFieldKind.LITERAL);
    }

    @Override
    protected int encodeStep(int idx, DictionaryInfo info, Consumer<TripletSupplier> output) {
        int ctx = info.getContext();
        int cnt = info.getContent();
        int leng = info.getLength();

        int dist = cnt == -1 ? 0 : ctx - cnt;

        if (dist == 0 && leng == 0) {
            // flag 0
            byte b = sequence().byteAt(idx);
            dictionary().update(idx, 1);
            output.accept(visitor -> {
                visitor.write(flagField, 0);
                visitor.write(byteField, b & 0xFF);
            });
            return idx + 1;
        } else {
            // flag 1
            return encodeStepSpecific(idx, output, leng, dist);
        }
    }

    protected abstract int encodeStepSpecific(int idx, Consumer<TripletSupplier> output, int leng, int dist);

    @Override
    protected int decodeStep(int idx, TripletProcessor input) throws MalformedStreamException {
        ByteBuilder builder = ((ByteBuilder) sequence());
        int flag = input.read(flagField);
        if (flag == -1) {
            return Integer.MAX_VALUE;
        }

        if (flag == 0) {
            byte b = (byte) requireField(input.read(byteField));
            builder.append(b);
            dictionary().update(idx, 1);

            return idx + 1;
        } else {
            int tempDist = requireField(input.read(distField));
            int dist = signedDistance(tempDist);
            int leng = requireField(input.read(lengField));

            int ctx = dictionary().searchContext(idx);
            int cnt = ctx - dist;

            byte[] seq = dictionary().copy(cnt, leng);
            builder.append(seq);

            return decodeStepSpecific(idx, leng, builder, input);
        }
    }

    protected abstract int decodeStepSpecific(int idx, int leng, ByteBuilder builder, TripletProcessor input)
            throws MalformedStreamException;

    public static final class SalomonByteless extends SalomonTripletCoder {

        public SalomonByteless(ByteSequence sequence, Dictionary dictionary, int distanceBits, int lengthBits) {
            super(sequence, dictionary, distanceBits, lengthBits);
        }

        @Override
        protected int encodeStepSpecific(int idx, Consumer<TripletSupplier> output, int leng, int dist) {
            dictionary().update(idx, leng);
            output.accept(visitor -> {
                visitor.write(super.flagField, 1);
                visitor.write(super.distField, dist & distanceMask());
                visitor.write(super.lengField, leng);
            });
            return idx + leng;
        }

        @Override
        protected int decodeStepSpecific(int idx, int leng, ByteBuilder builder, TripletProcessor input)
                throws MalformedStreamException {
            dictionary().update(idx, leng);
            return idx + leng;
        }
    }

    public static final class SalomonByteful extends SalomonTripletCoder {

        public SalomonByteful(ByteSequence sequence, Dictionary dictionary, int distanceBits, int lengthBits) {
            super(sequence, dictionary, distanceBits, lengthBits);
        }

        @Override
        protected int encodeStepSpecific(int idx, Consumer<TripletSupplier> output, int leng, int dist) {
            int leng2 = leng + idx == sequence().length() ? leng - 1 : leng;
            dictionary().update(idx, leng2 + 1);
            byte b = sequence().byteAt(idx + leng2);
            output.accept(visitor -> {
                visitor.write(super.flagField, 1);
                visitor.write(super.distField, dist & distanceMask());
                visitor.write(super.lengField, leng2);
                visitor.write(super.byteField, b & 0xFF);
            });
            return idx + leng2 + 1;
        }

        @Override
        protected int decodeStepSpecific(int idx, int leng, ByteBuilder builder, TripletProcessor input)
                throws MalformedStreamException {
            byte b = (byte) requireField(input.read(super.byteField));
            builder.append(b);
            dictionary().update(idx, leng + 1);
            return idx + leng + 1;
        }
    }
}
