package cz.cvut.fit.acb.fixtures;

import cz.cvut.fit.acb.coding.FieldCost;
import cz.cvut.fit.acb.coding.TripletWriter;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.triplets.FieldSource;
import cz.cvut.fit.acb.triplets.TripletFieldId;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Records every field the encoder writes into each block and checks that the decoder reads the same
 * fields back from that block in the same order, so a mismatch names the first diverging triplet
 * rather than only the final byte difference. A block is found by its coded bytes; blocks stored
 * raw are never read, so their fields are never checked. Wire it in through
 * {@link InterceptingProvider}.
 */
public final class TripletLog {

    private final Map<ByteBuffer, List<RecordedField>> blocks = new HashMap<>();

    public TripletWriter recording(TripletWriter delegate) {
        return new Recorder(delegate);
    }

    public FieldSource checking(FieldSource delegate, byte[] block) {
        List<RecordedField> written = this.blocks.get(ByteBuffer.wrap(block));
        assertThat(written).as("the decoder reads only blocks the encoder wrote").isNotNull();
        return new Checker(delegate, written);
    }

    private final class Recorder implements TripletWriter {

        private final TripletWriter delegate;
        private final List<RecordedField> fields = new ArrayList<>();

        private Recorder(TripletWriter delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(TripletFieldId field, int value) {
            this.fields.add(RecordedField.of(field, value));
            this.delegate.write(field, value);
        }

        @Override
        public byte[] finish() {
            byte[] block = this.delegate.finish();
            TripletLog.this.blocks.put(ByteBuffer.wrap(block.clone()), List.copyOf(this.fields));
            return block;
        }

        @Override
        public List<FieldCost> costs() {
            return this.delegate.costs();
        }
    }

    private static final class Checker implements FieldSource {

        private final FieldSource delegate;
        private final List<RecordedField> written;
        private int next;

        private Checker(FieldSource delegate, List<RecordedField> written) {
            this.delegate = delegate;
            this.written = written;
        }

        @Override
        public int read(TripletFieldId field) throws MalformedStreamException {
            int value = this.delegate.read(field);
            assertThat(this.next).as("the decoder reads no more fields than the encoder wrote")
                    .isLessThan(this.written.size());
            assertThat(RecordedField.of(field, value)).as("field %d of the block", this.next)
                    .isEqualTo(this.written.get(this.next++));
            return value;
        }
    }

    private record RecordedField(int index, int bitSize, boolean length, int value) {

        static RecordedField of(TripletFieldId field, int value) {
            return new RecordedField(field.index(), field.bitSize(), field.isLength(), value);
        }
    }
}
