package cz.cvut.fit.acb.format;

import java.util.Arrays;
import java.util.Objects;

import cz.cvut.fit.acb.ACBProviderParameters;
import cz.cvut.fit.acb.ACBProviderParameters.CoderE;
import cz.cvut.fit.acb.ACBProviderParameters.OrderStatisticTreeE;
import cz.cvut.fit.acb.ACBProviderParameters.TripletCoderE;

/**
 * The settings a stream was coded with - everything decoding depends on. The dictionary structure
 * is left out: every structure yields the same ranks, so it only affects speed.
 */
public record StreamHeader(int distanceBits, int lengthBits, TripletCoderE tripletCoding, CoderE entropyCoding,
                           int[] lengthFrequencies) {
	
	public StreamHeader {
		lengthFrequencies = lengthFrequencies.clone();
	}
	
	public static StreamHeader of(ACBProviderParameters params) {
		return new StreamHeader(params.distanceBits, params.lengthBits, params.tc, params.cd, params.lengthFrequencies);
	}
	
	@Override
	public int[] lengthFrequencies() {
		return this.lengthFrequencies.clone();
	}
	
	public ACBProviderParameters toParameters(OrderStatisticTreeE dictionaryStructure) {
		ACBProviderParameters params = new ACBProviderParameters();
		params.distanceBits = this.distanceBits;
		params.lengthBits = this.lengthBits;
		params.tc = this.tripletCoding;
		params.cd = this.entropyCoding;
		params.tr = dictionaryStructure;
		params.lengthFrequencies = this.lengthFrequencies.clone();
		return params;
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
