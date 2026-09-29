package cz.cvut.fit.acb;

import cz.cvut.fit.acb.fixtures.InterceptingProvider;
import cz.cvut.fit.acb.fixtures.SettingsCombination;
import cz.cvut.fit.acb.fixtures.TripletLog;
import cz.cvut.fit.acb.format.Block;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Whatever bytes sit in a valid container, decoding ends in success or {@link MalformedStreamException}. */
class CorruptPayloadPropertiesTest {

    private static final byte[] SAMPLE = "abracadabra abracadabra, cadabra abra".getBytes();

    @Property(tries = 500)
    void randomCodedBlocksDecodeOrAreMalformed(@ForAll("settings") CompressionSettings settings,
                                               @ForAll @IntRange(min = 1, max = 64) int rawLength,
                                               @ForAll("arrays") List<byte[]> arrays) {
        List<Block> blocks = arrays.stream().<Block>map(array -> Block.coded(rawLength, array)).toList();

        decodeOrReject(settings, new CompressedStream(StreamHeader.of(settings), blocks));
    }

    @Property(tries = 500)
    void aMutatedBlockDecodesOrIsMalformed(@ForAll("settings") CompressionSettings settings,
                                           @ForAll("mutations") List<int[]> mutations,
                                           @ForAll @IntRange(min = 0, max = 3) int dropped) {
        CompressionSettings small = settings.withSegmentSize(16);
        CompressedStream sound = new Compressor(small, InterceptingProvider.logging(new TripletLog()))
                .compress(SAMPLE);
        List<Block> blocks = new ArrayList<>(sound.blocks());
        for (int[] mutation : mutations) {
            int which = mutation[0] % blocks.size();
            Block block = blocks.get(which);
            byte[] bytes = block.bytes();
            if (bytes.length > 0) {
                bytes[mutation[1] % bytes.length] ^= (byte) mutation[2];
            }
            blocks.set(which, Block.coded(block.rawLength(), bytes));
        }
        for (int i = 0; i < dropped && blocks.size() > 1; i++) {
            blocks.removeLast();
        }

        decodeOrReject(settings, new CompressedStream(sound.header(), blocks));
    }

    private static void decodeOrReject(CompressionSettings settings, CompressedStream stream) {
        try {
            new Compressor(settings).decompress(stream);
        } catch (MalformedStreamException expected) {
            // the only failure a bad block may cause
        }
    }

    @Provide
    Arbitrary<CompressionSettings> settings() {
        Arbitrary<SettingsCombination> combinations = Arbitraries.of(SettingsCombination.all()
                .collect(Collectors.toList()));
        return Combinators.combine(combinations,
                Arbitraries.integers().between(1, 8), Arbitraries.integers().between(1, 8))
                .as((combination, distance, length) -> combination.settings()
                        .withDistanceBits(distance).withLengthBits(length));
    }

    @Provide
    Arbitrary<List<byte[]>> arrays() {
        return Arbitraries.bytes().array(byte[].class).ofMaxSize(40).list().ofMinSize(1).ofMaxSize(5);
    }

    @Provide
    Arbitrary<List<int[]>> mutations() {
        return Combinators.combine(Arbitraries.integers().between(0, 1000),
                        Arbitraries.integers().between(0, 1000), Arbitraries.integers().between(1, 255))
                .as((array, offset, mask) -> new int[]{array, offset, mask})
                .list().ofMinSize(1).ofMaxSize(4);
    }
}
