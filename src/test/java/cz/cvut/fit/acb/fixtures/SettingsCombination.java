package cz.cvut.fit.acb.fixtures;

import cz.cvut.fit.acb.CompressionSettings;
import cz.cvut.fit.acb.DictionaryStructure;
import cz.cvut.fit.acb.EntropyCoding;
import cz.cvut.fit.acb.TripletCoding;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

/** One choice of triplet coding, dictionary structure and entropy coding, at default bit widths. */
public record SettingsCombination(TripletCoding tripletCoding, DictionaryStructure dictionaryStructure,
                                  EntropyCoding entropyCoding) {

    public static Stream<SettingsCombination> all() {
        return Arrays.stream(TripletCoding.values())
                .flatMap(tc -> Arrays.stream(DictionaryStructure.values())
                        .flatMap(tr -> Arrays.stream(EntropyCoding.values())
                                .map(cd -> new SettingsCombination(tc, tr, cd))));
    }

    /**
     * One combination per coder and entropy coding on the red-black tree, and the other trees under
     * one coder. The quick tests use it; {@link #all()} is for the full run.
     */
    public static Stream<SettingsCombination> representative() {
        Stream<SettingsCombination> coders = Arrays.stream(TripletCoding.values())
                .flatMap(tc -> Arrays.stream(EntropyCoding.values())
                        .map(cd -> new SettingsCombination(tc, DictionaryStructure.RED_BLACK, cd)));
        Stream<SettingsCombination> trees = Stream.of(DictionaryStructure.BST, DictionaryStructure.ST)
                .map(tr -> new SettingsCombination(TripletCoding.VALACH, tr, EntropyCoding.ADAPTIVE_ARITHMETIC));
        return Stream.concat(coders, trees);
    }

    /** Combinations with an open {@code TODO.md} entry; tests skip them with this reason. */
    public Optional<String> knownRoundTripDefect() {
        return this.knownDictionaryDefect();
    }

    /** The same for one degenerate input; the combination may still be sound on every other input. */
    public Optional<String> knownRoundTripDefect(DegenerateInput input) {
        if (this.dictionaryStructure == DictionaryStructure.BST && input.isLongRun()) {
            return Optional.of("-ds bst overflows the stack on a long run of one byte (TODO.md)");
        }
        return this.knownRoundTripDefect();
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
