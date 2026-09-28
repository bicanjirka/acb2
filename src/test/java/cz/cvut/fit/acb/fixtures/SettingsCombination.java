package cz.cvut.fit.acb.fixtures;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import cz.cvut.fit.acb.CompressionSettings;
import cz.cvut.fit.acb.DictionaryStructure;
import cz.cvut.fit.acb.EntropyCoding;
import cz.cvut.fit.acb.TripletCoding;

/** One choice of triplet coding, dictionary structure and entropy coding, at default bit widths. */
public record SettingsCombination(TripletCoding tripletCoding, DictionaryStructure dictionaryStructure,
                                  EntropyCoding entropyCoding) {

	public static Stream<SettingsCombination> all() {
		return Arrays.stream(TripletCoding.values())
				.flatMap(tc -> Arrays.stream(DictionaryStructure.values())
						.flatMap(tr -> Arrays.stream(EntropyCoding.values())
								.map(cd -> new SettingsCombination(tc, tr, cd))));
	}

	/** Combinations with an open {@code TODO.md} entry; tests skip them with this reason. */
	public Optional<String> knownRoundTripDefect() {
		return this.knownDictionaryDefect();
	}

	/** Combinations whose decoder dictionary diverges from the encoder's; see {@code TODO.md}. */
	public Optional<String> knownDictionaryDefect() {
		if (this.tripletCoding == TripletCoding.LCP) {
			return Optional.of("LCP dictionary diverges between encoder and decoder (TODO.md)");
		}
		return Optional.empty();
	}

	public CompressionSettings settings() {
		return CompressionSettings.defaults()
				.withTripletCoding(this.tripletCoding)
				.withDictionaryStructure(this.dictionaryStructure)
				.withEntropyCoding(this.entropyCoding);
	}


	@Override
	public String toString() {
		return this.tripletCoding + "/" + this.dictionaryStructure + "/" + this.entropyCoding;
	}
}
