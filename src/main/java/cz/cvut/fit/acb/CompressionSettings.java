package cz.cvut.fit.acb;

import cz.cvut.fit.acb.coding.LengthFrequencies;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Everything that shapes compression. Start from {@link #defaults()} and change what differs with
 * the {@code withX} copies; the constructor rejects values no stream can carry.
 */
public record CompressionSettings(int distanceBits, int lengthBits, TripletCoding tripletCoding,
                                  EntropyCoding entropyCoding, LengthFrequencies lengthFrequencies,
                                  int segmentSize, int contextDepth) {

    /** Wider fields only inflate the range coder's model: 2^bits symbols each; 24 bits took 98% of a text. */
    public static final int MAX_FIELD_BITS = 16;

    /** Bytes of context that decide the order of the dictionary; the header stores it as one byte. */
    public static final int MAX_CONTEXT_DEPTH = 255;

    private static final CompressionSettings DEFAULTS = new CompressionSettings(6, 7, TripletCoding.VALACH,
            EntropyCoding.ADAPTIVE_ARITHMETIC, LengthFrequencies.flat(), 1_000_000, 10);

    public CompressionSettings {
        requireFieldBits("distance", distanceBits);
        requireFieldBits("length", lengthBits);
        Objects.requireNonNull(tripletCoding, "tripletCoding");
        Objects.requireNonNull(entropyCoding, "entropyCoding");
        Objects.requireNonNull(lengthFrequencies, "lengthFrequencies").requireFits(lengthAlphabetSize(lengthBits));
        if (segmentSize < 1) {
            throw new IllegalArgumentException("segment size must be greater than zero: " + segmentSize);
        }
        if (contextDepth < 1 || contextDepth > MAX_CONTEXT_DEPTH) {
            throw new IllegalArgumentException("context depth must be between 1 and " + MAX_CONTEXT_DEPTH + ": "
                    + contextDepth);
        }
    }

    /** Symbols of a length field's model: every length its width can hold. */
    public static int lengthAlphabetSize(int lengthBits) {
        return 1 << lengthBits;
    }

    public static CompressionSettings defaults() {
        return DEFAULTS;
    }

    public CompressionSettings withDistanceBits(int bits) {
        return this.copy(draft -> draft.distanceBits = bits);
    }

    public CompressionSettings withLengthBits(int bits) {
        return this.copy(draft -> draft.lengthBits = bits);
    }

    public CompressionSettings withTripletCoding(TripletCoding coding) {
        return this.copy(draft -> draft.tripletCoding = coding);
    }

    public CompressionSettings withEntropyCoding(EntropyCoding coding) {
        return this.copy(draft -> draft.entropyCoding = coding);
    }

    public CompressionSettings withLengthFrequencies(LengthFrequencies frequencies) {
        return this.copy(draft -> draft.lengthFrequencies = frequencies);
    }

    public CompressionSettings withLengthFrequencies(int... frequencies) {
        return this.withLengthFrequencies(LengthFrequencies.of(frequencies));
    }

    public CompressionSettings withContextDepth(int depth) {
        return this.copy(draft -> draft.contextDepth = depth);
    }

    public CompressionSettings withSegmentSize(int size) {
        return this.copy(draft -> draft.segmentSize = size);
    }

    /** Furthest a content may lie from its context, in dictionary ranks. */
    public int maxDistance() {
        return 1 << (this.distanceBits - 1);
    }

    /** Longest match a triplet can carry. */
    public int maxLength() {
        return (1 << this.lengthBits) - 1;
    }

    private static void requireFieldBits(String field, int bits) {
        if (bits < 1 || bits > MAX_FIELD_BITS) {
            throw new IllegalArgumentException(field + " bits must be between 1 and " + MAX_FIELD_BITS + ": " + bits);
        }
    }

    /** Adding a component means adding it here and to the record, and nowhere else. */
    private CompressionSettings copy(Consumer<Draft> change) {
        Draft draft = new Draft(this);
        change.accept(draft);
        return draft.build();
    }

    /** The components of a copy while it is being changed. */
    private static final class Draft {

        private int distanceBits;
        private int lengthBits;
        private TripletCoding tripletCoding;
        private EntropyCoding entropyCoding;
        private LengthFrequencies lengthFrequencies;
        private int segmentSize;
        private int contextDepth;

        private Draft(CompressionSettings from) {
            this.distanceBits = from.distanceBits;
            this.lengthBits = from.lengthBits;
            this.tripletCoding = from.tripletCoding;
            this.entropyCoding = from.entropyCoding;
            this.lengthFrequencies = from.lengthFrequencies;
            this.segmentSize = from.segmentSize;
            this.contextDepth = from.contextDepth;
        }

        private CompressionSettings build() {
            return new CompressionSettings(this.distanceBits, this.lengthBits, this.tripletCoding,
                    this.entropyCoding, this.lengthFrequencies, this.segmentSize, this.contextDepth);
        }
    }
}
