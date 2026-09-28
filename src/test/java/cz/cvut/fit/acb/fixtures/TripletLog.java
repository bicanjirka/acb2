package cz.cvut.fit.acb.fixtures;

import cz.cvut.fit.acb.coding.TripletWriter;
import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletProcessor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Records every triplet field the encoder writes and checks that the decoder reads back the same
 * fields in the same order, so a mismatch names the first diverging triplet rather than only the
 * final byte difference. Wire it in through {@link InterceptingProvider}.
 */
public final class TripletLog {

    private final Deque<RecordedField> written = new ArrayDeque<>();
    private OptionalInt size = OptionalInt.empty();

    public TripletWriter recording(TripletWriter delegate) {
        return new Recorder(delegate);
    }

    public TripletProcessor checking(TripletProcessor delegate) {
        return new Checker(delegate);
    }

    private final class Recorder implements TripletWriter {

        private final TripletWriter delegate;

        private Recorder(TripletWriter delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read(TripletFieldId fieldId) {
            throw new AssertionError("The encoder must not read triplets");
        }

        @Override
        public void write(TripletFieldId fieldId, int value) {
            TripletLog.this.written.add(RecordedField.of(fieldId, value));
            this.delegate.write(fieldId, value);
        }

        @Override
        public int getSize() {
            throw new AssertionError("The encoder must not read the size");
        }

        @Override
        public void setSize(int size) {
            assertThat(TripletLog.this.size).as("the size is announced once").isEmpty();
            TripletLog.this.size = OptionalInt.of(size);
            this.delegate.setSize(size);
        }

        @Override
        public List<byte[]> finish() {
            return this.delegate.finish();
        }
    }

    private final class Checker implements TripletProcessor {

        private final TripletProcessor delegate;

        private Checker(TripletProcessor delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read(TripletFieldId fieldId) {
            int value = this.delegate.read(fieldId);
            if (value == -1) {
                return value;
            }
            assertThat(RecordedField.of(fieldId, value)).isEqualTo(TripletLog.this.written.poll());
            return value;
        }

        @Override
        public void write(TripletFieldId fieldId, int value) {
            throw new AssertionError("The decoder must not write triplets");
        }

        @Override
        public int getSize() {
            int size = this.delegate.getSize();
            assertThat(OptionalInt.of(size)).isEqualTo(TripletLog.this.size);
            return size;
        }

        @Override
        public void setSize(int size) {
            throw new AssertionError("The decoder must not announce a size");
        }
    }

    private record RecordedField(int index, int bitSize, boolean length, int value) {

        static RecordedField of(TripletFieldId fieldId, int value) {
            return new RecordedField(fieldId.index(), fieldId.bitSize(), fieldId.isLength(), value);
        }
    }
}
