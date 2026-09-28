package cz.cvut.fit.acb.fixtures;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import cz.cvut.fit.acb.ACBProvider;
import cz.cvut.fit.acb.ACBProviderImpl;
import cz.cvut.fit.acb.ACBProviderParameters;
import cz.cvut.fit.acb.ACBProviderParameters.CoderE;
import cz.cvut.fit.acb.ACBProviderParameters.OrderStatisticTreeE;
import cz.cvut.fit.acb.ACBProviderParameters.TripletCoderE;

/** One choice of triplet coding, dictionary structure and entropy coding, at default bit widths. */
public record SettingsCombination(TripletCoderE tripletCoding, OrderStatisticTreeE dictionaryStructure,
                                  CoderE entropyCoding) {

	public static Stream<SettingsCombination> all() {
		return Arrays.stream(TripletCoderE.values())
				.flatMap(tc -> Arrays.stream(OrderStatisticTreeE.values())
						.flatMap(tr -> Arrays.stream(CoderE.values())
								.map(cd -> new SettingsCombination(tc, tr, cd))));
	}

	/** Combinations with an open {@code TODO.md} entry; tests skip them with this reason. */
	public Optional<String> knownRoundTripDefect() {
		return this.knownDictionaryDefect().or(() -> this.tripletCoding == TripletCoderE.VALACH
				? Optional.of("Valach coding loses sync at some segment sizes (TODO.md)")
				: Optional.empty());
	}

	/** Combinations whose decoder dictionary diverges from the encoder's; see {@code TODO.md}. */
	public Optional<String> knownDictionaryDefect() {
		if (this.entropyCoding == CoderE.BIT_ARRAY) {
			return Optional.of("Bit-array decoding reads trailing padding as triplets (TODO.md)");
		}
		if (this.tripletCoding == TripletCoderE.LCP) {
			return Optional.of("LCP dictionary diverges between encoder and decoder (TODO.md)");
		}
		return Optional.empty();
	}

	public ACBProvider provider() {
		ACBProviderParameters params = new ACBProviderParameters();
		params.tc = this.tripletCoding;
		params.tr = this.dictionaryStructure;
		params.cd = this.entropyCoding;
		return new ACBProviderImpl(params);
	}

	@Override
	public String toString() {
		return this.tripletCoding + "/" + this.dictionaryStructure + "/" + this.entropyCoding;
	}
}
