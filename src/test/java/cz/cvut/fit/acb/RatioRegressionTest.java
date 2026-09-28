package cz.cvut.fit.acb;

import cz.cvut.fit.acb.fixtures.GeneratedInput;
import cz.cvut.fit.acb.format.ContainerFormat;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the compression ratio: a refactor that makes a coder's output larger than its pin by more
 * than 1% fails the build. Pins are container sizes with a 7-bit length field on the generated
 * inputs. A phase that improves the ratio lowers them, measured with the ratio harness, in the
 * same commit.
 */
class RatioRegressionTest {

    private static final double TOLERANCE = 1.01;
    private static final int LENGTH_BITS = 7;

    record Pin(String input, Supplier<GeneratedInput> source, TripletCoding coding, EntropyCoding entropy,
               int bytes) {

        @Override
        public String toString() {
            return this.input + " " + this.coding + " " + this.entropy;
        }
    }

    static Stream<Pin> pins() {
        Supplier<GeneratedInput> text = () -> GeneratedInput.text(60_000);
        Supplier<GeneratedInput> binary = () -> GeneratedInput.binary(30_000);
        EntropyCoding arith = EntropyCoding.ADAPTIVE_ARITHMETIC;
        EntropyCoding bits = EntropyCoding.BIT_ARRAY;
        return Stream.of(
                new Pin("text", text, TripletCoding.SALOMON, arith, 17_229),
                new Pin("text", text, TripletCoding.SALOMON, bits, 26_197),
                new Pin("text", text, TripletCoding.SALOMON2, arith, 15_493),
                new Pin("text", text, TripletCoding.SALOMON2, bits, 25_161),
                new Pin("text", text, TripletCoding.SIMPLE, arith, 15_590),
                new Pin("text", text, TripletCoding.SIMPLE, bits, 24_645),
                new Pin("text", text, TripletCoding.VALACH, arith, 15_472),
                new Pin("text", text, TripletCoding.VALACH, bits, 24_342),
                new Pin("binary", binary, TripletCoding.SALOMON, arith, 26_105),
                new Pin("binary", binary, TripletCoding.SALOMON, bits, 32_597),
                new Pin("binary", binary, TripletCoding.SALOMON2, arith, 26_004),
                new Pin("binary", binary, TripletCoding.SALOMON2, bits, 31_714),
                new Pin("binary", binary, TripletCoding.SIMPLE, arith, 27_710),
                new Pin("binary", binary, TripletCoding.SIMPLE, bits, 46_429),
                new Pin("binary", binary, TripletCoding.VALACH, arith, 25_995),
                new Pin("binary", binary, TripletCoding.VALACH, bits, 38_617));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pins")
    void aCoderDoesNotCompressWorseThanItsPin(Pin pin) {
        byte[] input = pin.source().get().bytes();
        CompressionSettings settings = CompressionSettings.defaults().withTripletCoding(pin.coding())
                .withEntropyCoding(pin.entropy()).withLengthBits(LENGTH_BITS);

        int size = ContainerFormat.encode(new Compressor(settings).compress(input)).length;

        assertThat(size).as("container bytes for %s", pin).isLessThanOrEqualTo((int) (pin.bytes() * TOLERANCE));
    }
}
