package cz.cvut.fit.acb.format;

import java.util.Arrays;
import java.util.Objects;

import cz.cvut.fit.acb.CompressionSettings;
import cz.cvut.fit.acb.DictionaryStructure;
import cz.cvut.fit.acb.EntropyCoding;
import cz.cvut.fit.acb.TripletCoding;

/**
 * The settings a stream was coded with - everything decoding depends on. The dictionary structure
 * is left out, since it only affects speed, and so is the segment size, which the payload records.
 */
public record StreamHeader(int distanceBits, int lengthBits, TripletCoding tripletCoding, EntropyCoding entropyCoding,
                           int[] lengthFrequencies) {

	public StreamHeader {
		lengthFrequencies = lengthFrequencies.clone();
	}

	public static StreamHeader of(CompressionSettings settings) {
		return new StreamHeader(settings.distanceBits(), settings.lengthBits(), settings.tripletCoding(),
				settings.entropyCoding(), settings.lengthFrequencies());
	}

	@Override
	public int[] lengthFrequencies() {
		return this.lengthFrequencies.clone();
	}

	/**
	 * @throws IllegalArgumentException if a value is out of the range {@link CompressionSettings} accepts
	 */
	public CompressionSettings toSettings(DictionaryStructure dictionaryStructure) {
		return CompressionSettings.defaults()
				.withDistanceBits(this.distanceBits)
				.withLengthBits(this.lengthBits)
				.withTripletCoding(this.tripletCoding)
				.withEntropyCoding(this.entropyCoding)
				.withLengthFrequencies(this.lengthFrequencies)
				.withDictionaryStructure(dictionaryStructure);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof StreamHeader that
				&& this.distanceBits == that.distanceBits
				&& this.lengthBits == that.lengthBits
				&& this.tripletCoding == that.tripletCoding
				&& this.entropyCoding == that.entropyCoding
				&& Arrays.equals(this.lengthFrequencies, that.lengthFrequencies);
	}

	@Override
	public int hashCode() {
		return 31 * Objects.hash(this.distanceBits, this.lengthBits, this.tripletCoding, this.entropyCoding)
				+ Arrays.hashCode(this.lengthFrequencies);
	}

	@Override
	public String toString() {
		return "StreamHeader[distanceBits=" + this.distanceBits + ", lengthBits=" + this.lengthBits
				+ ", tripletCoding=" + this.tripletCoding + ", entropyCoding=" + this.entropyCoding
				+ ", lengthFrequencies=" + Arrays.toString(this.lengthFrequencies) + "]";
	}
}
