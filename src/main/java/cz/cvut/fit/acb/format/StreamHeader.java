package cz.cvut.fit.acb.format;

import cz.cvut.fit.acb.CompressionSettings;
import cz.cvut.fit.acb.EntropyCoding;
import cz.cvut.fit.acb.TripletCoding;

import java.util.Arrays;
import java.util.Objects;

/**
 * The settings a stream was coded with: everything decoding depends on, and the segment size that
 * bounds every block. Frequencies beyond the length alphabet are dropped, since the coder ignores
 * them.
 */
public record StreamHeader(int distanceBits, int lengthBits, TripletCoding tripletCoding, EntropyCoding entropyCoding,
                           int[] lengthFrequencies, int segmentSize) {

    public StreamHeader {
        lengthFrequencies = lengthFrequencies.clone();
    }

    public static StreamHeader of(CompressionSettings settings) {
        int alphabet = CompressionSettings.lengthAlphabetSize(settings.lengthBits());
        return new StreamHeader(settings.distanceBits(), settings.lengthBits(), settings.tripletCoding(),
                settings.entropyCoding(), Arrays.stream(settings.lengthFrequencies()).limit(alphabet).toArray(),
                settings.segmentSize());
    }

    @Override
    public int[] lengthFrequencies() {
        return this.lengthFrequencies.clone();
    }

    /**
     * @throws IllegalArgumentException if a value is out of the range {@link CompressionSettings} accepts
     */
    public CompressionSettings toSettings() {
        return CompressionSettings.defaults()
                .withDistanceBits(this.distanceBits)
                .withLengthBits(this.lengthBits)
                .withTripletCoding(this.tripletCoding)
                .withEntropyCoding(this.entropyCoding)
                .withLengthFrequencies(this.lengthFrequencies)
                .withSegmentSize(this.segmentSize);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof StreamHeader that
                && this.distanceBits == that.distanceBits
                && this.lengthBits == that.lengthBits
                && this.tripletCoding == that.tripletCoding
                && this.entropyCoding == that.entropyCoding
                && Arrays.equals(this.lengthFrequencies, that.lengthFrequencies)
                && this.segmentSize == that.segmentSize;
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(this.distanceBits, this.lengthBits, this.tripletCoding, this.entropyCoding,
                this.segmentSize) + Arrays.hashCode(this.lengthFrequencies);
    }

    @Override
    public String toString() {
        return "StreamHeader[distanceBits=" + this.distanceBits + ", lengthBits=" + this.lengthBits
                + ", tripletCoding=" + this.tripletCoding + ", entropyCoding=" + this.entropyCoding
                + ", lengthFrequencies=" + Arrays.toString(this.lengthFrequencies)
                + ", segmentSize=" + this.segmentSize + "]";
    }
}
