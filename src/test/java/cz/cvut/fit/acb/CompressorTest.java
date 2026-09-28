package cz.cvut.fit.acb;

import cz.cvut.fit.acb.format.CompressedStream;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.format.StreamHeader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompressorTest {

    private final Compressor compressor = new Compressor(CompressionSettings.defaults());

    @Test
    void aStreamRecordsTheSettingsItWasCompressedWith() {
        CompressionSettings settings = CompressionSettings.defaults().withTripletCoding(TripletCoding.VALACH);

        CompressedStream stream = new Compressor(settings).compress(new byte[]{1, 2, 3});

        assertThat(stream.header()).isEqualTo(StreamHeader.of(settings));
    }

    @Test
    void aPayloadWithoutASegmentSizeIsMalformed() {
        CompressedStream stream = new CompressedStream(StreamHeader.of(CompressionSettings.defaults()), List.of());

        assertThatThrownBy(() -> this.compressor.decompress(stream)).isInstanceOf(MalformedStreamException.class);
    }

    @Test
    void aNegativeSegmentSizeIsMalformed() {
        CompressedStream stream = new CompressedStream(StreamHeader.of(CompressionSettings.defaults()),
                List.of(new byte[]{-1, -1, -1, -1}));

        assertThatThrownBy(() -> this.compressor.decompress(stream)).isInstanceOf(MalformedStreamException.class);
    }
}
