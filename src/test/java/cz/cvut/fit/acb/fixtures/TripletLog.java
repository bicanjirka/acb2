package cz.cvut.fit.acb.fixtures;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.OptionalInt;
import java.util.function.Consumer;

import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletProcessor;
import cz.cvut.fit.acb.triplets.TripletSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pipeline stages that record every triplet field the encoder writes and check that the decoder
 * reads back the same fields in the same order, so a mismatch names the first diverging triplet
 * rather than only the final byte difference.
 */
public final class TripletLog {

	private final Deque<RecordedField> written = new ArrayDeque<>();
	private OptionalInt size = OptionalInt.empty();

	/** Encoder side: records each supplier's fields, then forwards it unchanged. */
	public void recordWrites(TripletSupplier supplier, Consumer<TripletSupplier> next) {
		if (supplier != null) {
			supplier.visit(new Recorder());
		}
		next.accept(supplier);
	}

	/** Decoder side: forwards reads, asserting each against the next recorded field. */
	public void checkReads(TripletProcessor source, Consumer<TripletProcessor> next) {
		next.accept(new Checker(source));
	}

	private final class Recorder implements TripletProcessor {

		@Override
		public int read(TripletFieldId fieldId) {
			throw new AssertionError("The encoder must not read triplets");
		}

		@Override
		public void write(TripletFieldId fieldId, int value) {
			TripletLog.this.written.add(RecordedField.of(fieldId, value));
		}

		@Override
		public int getSize() {
			throw new AssertionError("The encoder must not read the size");
		}

		@Override
		public void setSize(int size) {
			assertThat(TripletLog.this.size).as("the size is announced once").isEmpty();
			TripletLog.this.size = OptionalInt.of(size);
		}
	}

	private final class Checker implements TripletProcessor {

		private final TripletProcessor source;

		private Checker(TripletProcessor source) {
			this.source = source;
		}

		@Override
		public int read(TripletFieldId fieldId) {
			int value = this.source.read(fieldId);
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
			int size = this.source.getSize();
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
			return new RecordedField(fieldId.getIndex(), fieldId.getBitSize(), fieldId.isLength(), value);
		}
	}
}
