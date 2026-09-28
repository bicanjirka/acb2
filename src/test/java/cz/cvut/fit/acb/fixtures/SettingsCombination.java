package cz.cvut.fit.acb.fixtures;

import cz.cvut.fit.acb.CompressionSettings;
import cz.cvut.fit.acb.EntropyCoding;
import cz.cvut.fit.acb.TripletCoding;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

/** One choice of triplet coding and entropy coding, at default bit widths. */
public record SettingsCombination(TripletCoding tripletCoding, EntropyCoding entropyCoding) {

    public static Stream<SettingsCombination> all() {
        return Arrays.stream(TripletCoding.values())
                .flatMap(tc -> Arrays.stream(EntropyCoding.values()).map(cd -> new SettingsCombination(tc, cd)));
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
                .withEntropyCoding(this.entropyCoding);
    }

    @Override
    public String toString() {
        return this.tripletCoding + "/" + this.entropyCoding;
    }
}
