package cz.cvut.fit.acb;

import java.util.Arrays;
import java.util.Objects;

/**
 * Everything that shapes compression. Start from {@link #defaults()} and change what differs with
 * the {@code withX} copies; the constructor rejects values no stream can carry.
 */
public record CompressionSettings(int distanceBits, int lengthBits, TripletCoding tripletCoding,
                                  DictionaryStructure dictionaryStructure, EntropyCoding entropyCoding,
                                  int[] lengthFrequencies, int segmentSize) {

	public static final int MAX_FIELD_BITS = 30;

	private static final CompressionSettings DEFAULTS = new CompressionSettings(6, 4, TripletCoding.SIMPLE,
			DictionaryStructure.RED_BLACK, EntropyCoding.ADAPTIVE_ARITHMETIC, new int[]{45, 13, 10, 7, 5, 4},
			1_000_000);

	public CompressionSettings {
		requireFieldBits("distance", distanceBits);
		requireFieldBits("length", lengthBits);
		Objects.requireNonNull(tripletCoding, "tripletCoding");
		Objects.requireNonNull(dictionaryStructure, "dictionaryStructure");
		Objects.requireNonNull(entropyCoding, "entropyCoding");
		lengthFrequencies = lengthFrequencies.clone();
		if (Arrays.stream(lengthFrequencies).anyMatch(f -> f <= 0)) {
			throw new IllegalArgumentException("length frequencies must be greater than zero: "
					+ Arrays.toString(lengthFrequencies));
		}
		if (segmentSize < 1) {
			throw new IllegalArgumentException("segment size must be greater than zero: " + segmentSize);
		}
	}

	public static CompressionSettings defaults() {
		return DEFAULTS;
	}

	public CompressionSettings withDistanceBits(int bits) {
		return new CompressionSettings(bits, this.lengthBits, this.tripletCoding, this.dictionaryStructure,
				this.entropyCoding, this.lengthFrequencies, this.segmentSize);
	}

	public CompressionSettings withLengthBits(int bits) {
		return new CompressionSettings(this.distanceBits, bits, this.tripletCoding, this.dictionaryStructure,
				this.entropyCoding, this.lengthFrequencies, this.segmentSize);
	}

	public CompressionSettings withTripletCoding(TripletCoding coding) {
		return new CompressionSettings(this.distanceBits, this.lengthBits, coding, this.dictionaryStructure,
				this.entropyCoding, this.lengthFrequencies, this.segmentSize);
	}

	public CompressionSettings withDictionaryStructure(DictionaryStructure structure) {
		return new CompressionSettings(this.distanceBits, this.lengthBits, this.tripletCoding, structure,
				this.entropyCoding, this.lengthFrequencies, this.segmentSize);
	}

	public CompressionSettings withEntropyCoding(EntropyCoding coding) {
		return new CompressionSettings(this.distanceBits, this.lengthBits, this.tripletCoding,
				this.dictionaryStructure, coding, this.lengthFrequencies, this.segmentSize);
	}

	public CompressionSettings withLengthFrequencies(int... frequencies) {
		return new CompressionSettings(this.distanceBits, this.lengthBits, this.tripletCoding,
				this.dictionaryStructure, this.entropyCoding, frequencies, this.segmentSize);
	}

	public CompressionSettings withSegmentSize(int size) {
		return new CompressionSettings(this.distanceBits, this.lengthBits, this.tripletCoding,
				this.dictionaryStructure, this.entropyCoding, this.lengthFrequencies, size);
	}

	@Override
	public int[] lengthFrequencies() {
		return this.lengthFrequencies.clone();
	}

	/** Furthest a content may lie from its context, in dictionary ranks. */
	public int maxDistance() {
		return 1 << (this.distanceBits - 1);
	}

	/** Longest match a triplet can carry. */
	public int maxLength() {
		return (1 << this.lengthBits) - 1;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof CompressionSettings that
				&& this.distanceBits == that.distanceBits
				&& this.lengthBits == that.lengthBits
				&& this.tripletCoding == that.tripletCoding
				&& this.dictionaryStructure == that.dictionaryStructure
				&& this.entropyCoding == that.entropyCoding
				&& Arrays.equals(this.lengthFrequencies, that.lengthFrequencies)
				&& this.segmentSize == that.segmentSize;
	}

	@Override
	public int hashCode() {
		return 31 * Objects.hash(this.distanceBits, this.lengthBits, this.tripletCoding, this.dictionaryStructure,
				this.entropyCoding, this.segmentSize) + Arrays.hashCode(this.lengthFrequencies);
	}

	@Override
	public String toString() {
		return "CompressionSettings[distanceBits=" + this.distanceBits + ", lengthBits=" + this.lengthBits
				+ ", tripletCoding=" + this.tripletCoding + ", dictionaryStructure=" + this.dictionaryStructure
				+ ", entropyCoding=" + this.entropyCoding
				+ ", lengthFrequencies=" + Arrays.toString(this.lengthFrequencies)
				+ ", segmentSize=" + this.segmentSize + "]";
	}

	private static void requireFieldBits(String field, int bits) {
		if (bits < 1 || bits > MAX_FIELD_BITS) {
			throw new IllegalArgumentException(field + " bits must be between 1 and " + MAX_FIELD_BITS + ": " + bits);
		}
	}
}
