package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.format.MalformedStreamException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArithmeticFieldTest {

    @Test
    void aFieldReadsItsSymbolsAndThenKeepsReportingTheEnd() throws MalformedStreamException {
        AdaptiveArithmeticCompress encoder = new AdaptiveArithmeticCompress(4);
        encoder.compress(3);
        encoder.compress(15);
        encoder.compress(0);
        encoder.terminate();

        AdaptiveArithmeticDecompress decoder = new AdaptiveArithmeticDecompress(4, encoder.array());

        assertThat(new int[]{decoder.decompress(), decoder.decompress(), decoder.decompress(),
                decoder.decompress(), decoder.decompress(), decoder.decompress()})
                .containsExactly(3, 15, 0, -1, -1, -1);
    }
}
