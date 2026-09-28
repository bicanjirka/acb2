package cz.cvut.fit.acb;

import cz.cvut.fit.acb.fixtures.SettingsCombination;
import cz.cvut.fit.acb.format.CompressedStream;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.format.StreamHeader;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Whatever bytes sit in a valid container, decoding ends in success or {@link MalformedStreamException}. */
class CorruptPayloadPropertiesTest {

    private static final byte[] SAMPLE = "abracadabra abracadabra, cadabra abra".getBytes();

    @Property(tries = 500)
    void randomPayloadsDecodeOrAreMalformed(@ForAll("settings") CompressionSettings settings,
                                            @ForAll @IntRange(min = 0, max = 64) int segmentSize,
                                            @ForAll("arrays") List<byte[]> arrays) {
        List<byte[]> payload = new ArrayList<>();
        payload.add(ByteBuffer.allocate(Integer.BYTES).putInt(segmentSize).array());
        payload.addAll(arrays);

        decodeOrReject(settings, new CompressedStream(StreamHeader.of(settings), payload));
    }

    @Property(tries = 500)
    void aMutatedPayloadDecodesOrIsMalformed(@ForAll("settings") CompressionSettings settings,
                                             @ForAll("mutations") List<int[]> mutations,
                                             @ForAll @IntRange(min = 0, max = 3) int dropped) {
        CompressedStream sound = new Compressor(settings.withSegmentSize(16)).compress(SAMPLE);
        List<byte[]> payload = new ArrayList<>();
        sound.payload().forEach(array -> payload.add(array.clone()));
        for (int[] mutation : mutations) {
            byte[] array = payload.get(1 + mutation[0] % (payload.size() - 1));
            if (array.length > 0) {
                array[mutation[1] % array.length] ^= (byte) mutation[2];
            }
        }
        for (int i = 0; i < dropped && payload.size() > 2; i++) {
            payload.removeLast();
        }

        decodeOrReject(settings, new CompressedStream(sound.header(), payload));
    }

    private static void decodeOrReject(CompressionSettings settings, CompressedStream stream) {
        try {
            new Compressor(settings).decompress(stream);
        } catch (MalformedStreamException expected) {
            // the only failure a bad payload may cause
        }
    }

    @Provide
    Arbitrary<CompressionSettings> settings() {
        Arbitrary<SettingsCombination> combinations = Arbitraries.of(SettingsCombination.all()
                .filter(combination -> combination.knownRoundTripDefect().isEmpty())
                .collect(Collectors.toList()));
        return Combinators.combine(combinations,
                Arbitraries.integers().between(1, 8), Arbitraries.integers().between(1, 8))
                .as((combination, distance, length) -> combination.settings()
                        .withDistanceBits(distance).withLengthBits(length));
    }

    @Provide
    Arbitrary<List<byte[]>> arrays() {
        return Arbitraries.bytes().array(byte[].class).ofMaxSize(40).list().ofMaxSize(5);
    }

    @Provide
    Arbitrary<List<int[]>> mutations() {
        return Combinators.combine(Arbitraries.integers().between(0, 1000),
                        Arbitraries.integers().between(0, 1000), Arbitraries.integers().between(1, 255))
                .as((array, offset, mask) -> new int[]{array, offset, mask})
                .list().ofMinSize(1).ofMaxSize(4);
    }
}
