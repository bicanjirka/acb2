package cz.cvut.fit.acb.fixtures;

import cz.cvut.fit.acb.CompressionSettings;
import cz.cvut.fit.acb.EntropyCoding;
import cz.cvut.fit.acb.TripletCoding;

import java.util.Arrays;
import java.util.stream.Stream;

/** One choice of triplet coding and entropy coding, at default bit widths. */
public record SettingsCombination(TripletCoding tripletCoding, EntropyCoding entropyCoding) {

    public static Stream<SettingsCombination> all() {
        return Arrays.stream(TripletCoding.values())
                .flatMap(tc -> Arrays.stream(EntropyCoding.values()).map(cd -> new SettingsCombination(tc, cd)));
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
