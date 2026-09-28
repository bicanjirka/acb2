package cz.cvut.fit.acb;

import cz.cvut.fit.acb.fixtures.PipelineFixtures;
import cz.cvut.fit.acb.fixtures.SettingsCombination;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

import static org.assertj.core.api.Assertions.assertThat;

class RoundTripPropertiesTest {

    @Property(tries = 300)
    void anyInputDecompressesToItself(@ForAll("settings") SettingsCombination settings,
                                      @ForAll("inputs") byte[] input,
                                      @ForAll @IntRange(min = 1, max = 64) int segmentSize) {
        byte[] decompressed = PipelineFixtures.roundTrip(settings.settings().withSegmentSize(segmentSize), input);

        assertThat(decompressed).isEqualTo(input);
    }

    /** Combinations with a known defect are left to {@code RoundTripTest}, which reports them skipped. */
    @Provide
    Arbitrary<SettingsCombination> settings() {
        return Arbitraries.of(SettingsCombination.all()
                .filter(settings -> settings.knownRoundTripDefect().isEmpty())
                .toList());
    }

    /** Uniform bytes rarely repeat, so half the inputs use a three-letter alphabet to force matches. */
    @Provide
    Arbitrary<byte[]> inputs() {
        Arbitrary<byte[]> anyBytes = Arbitraries.bytes().array(byte[].class).ofMaxSize(300);
        Arbitrary<byte[]> fewSymbols = Arbitraries.bytes().between((byte) 'a', (byte) 'c')
                .array(byte[].class).ofMaxSize(300);
        return Arbitraries.oneOf(anyBytes, fewSymbols);
    }
}
