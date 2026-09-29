package cz.cvut.fit.acb.format;

import cz.cvut.fit.acb.CompressionSettings;
import cz.cvut.fit.acb.EntropyCoding;
import cz.cvut.fit.acb.TripletCoding;
import cz.cvut.fit.acb.coding.LengthFrequencies;

/**
 * The settings a stream was coded with: everything decoding depends on, and the segment size that
 * bounds every block. Frequencies beyond the length alphabet are dropped, since the coder ignores
 * them.
 */
public record StreamHeader(int distanceBits, int lengthBits, TripletCoding tripletCoding, EntropyCoding entropyCoding,
                           LengthFrequencies lengthFrequencies, int segmentSize) {

    public static StreamHeader of(CompressionSettings settings) {
        int alphabet = CompressionSettings.lengthAlphabetSize(settings.lengthBits());
        return new StreamHeader(settings.distanceBits(), settings.lengthBits(), settings.tripletCoding(),
                settings.entropyCoding(), settings.lengthFrequencies().limitedTo(alphabet), settings.segmentSize());
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
}
