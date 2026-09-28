package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.format.MalformedStreamException;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RangeCoderTest {

    private static final int[] FREQUENCIES = {900, 60, 30, 10};
    private static final int[] CUMULATIVE = {0, 900, 960, 990};
    private static final int TOTAL = 1_000;

    @Test
    void aStaticDistributionIsCodedWithinAHalfPercentOfItsEntropy() {
        Random random = new Random(20260928L);
        RangeEncoder encoder = new RangeEncoder();
        double entropyBits = 0;

        for (int i = 0; i < 50_000; i++) {
            int draw = random.nextInt(TOTAL);
            int symbol = draw < 900 ? 0 : draw < 960 ? 1 : draw < 990 ? 2 : 3;
            encoder.encode(CUMULATIVE[symbol], FREQUENCIES[symbol], TOTAL);
            entropyBits -= Math.log((double) FREQUENCIES[symbol] / TOTAL) / Math.log(2);
        }
        encoder.finish();

        assertThat(encoder.toArray().length * 8.0).isBetween(entropyBits, entropyBits * 1.005 + 64);
    }

    @Test
    void aStreamOfNoSymbolsTakesAtMostTheFlush() {
        RangeEncoder encoder = new RangeEncoder();
        encoder.finish();

        assertThat(encoder.toArray().length).isLessThanOrEqualTo(Integer.BYTES);
    }

    @Test
    void aSymbolOutsideItsDistributionIsRefused() {
        RangeEncoder encoder = new RangeEncoder();

        assertThatThrownBy(() -> encoder.encode(5, 6, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> encoder.encode(0, 0, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> encoder.encode(0, 1, RangeEncoder.MAX_TOTAL + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aFinishedEncoderTakesNoMoreSymbols() {
        RangeEncoder encoder = new RangeEncoder();
        encoder.finish();

        assertThatThrownBy(() -> encoder.encode(0, 1, 2)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aDecoderRunningPastTheEndOfItsBytesReportsAMalformedStream() throws MalformedStreamException {
        RangeDecoder decoder = new RangeDecoder(new byte[0]);

        assertThatThrownBy(() -> {
            for (int i = 0; i < 100; i++) {
                decoder.consume(decoder.target(2), 1);
            }
        }).isInstanceOf(MalformedStreamException.class);
    }
}
