package cz.cvut.fit.acb;

import cz.cvut.fit.acb.fixtures.GeneratedInput;
import cz.cvut.fit.acb.fixtures.ThreadRecordingProvider;
import cz.cvut.fit.acb.format.Block;
import cz.cvut.fit.acb.format.CompressedStream;
import cz.cvut.fit.acb.format.ContainerFormat;
import cz.cvut.fit.acb.format.MalformedStreamException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParallelCompressionTest {

    private static final int SEGMENT_SIZE = 5_000;
    private static final int THREADS = 4;

    private static CompressionSettings settings(TripletCoding coding) {
        return CompressionSettings.defaults().withTripletCoding(coding).withSegmentSize(SEGMENT_SIZE);
    }

    @ParameterizedTest
    @EnumSource(TripletCoding.class)
    void aStreamCodedOnSeveralThreadsIsTheOneCodedOnOne(TripletCoding coding) throws MalformedStreamException {
        byte[] input = GeneratedInput.text(60_000).bytes();
        CompressedStream sequential = new Compressor(settings(coding)).compress(input);

        try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            Compressor parallel = new Compressor(settings(coding), ConfiguredACBProvider::new,
                    OrderedMapper.on(pool, 2 * THREADS));

            CompressedStream stream = parallel.compress(input);

            assertThat(stream.blocks()).hasSize(12);
            assertThat(ContainerFormat.encode(stream)).isEqualTo(ContainerFormat.encode(sequential));
            assertThat(parallel.decompress(stream)).isEqualTo(input);
        }
    }

    @Test
    void theStatsOfSegmentsCodedOnSeveralThreadsAddUpAsTheyDoOnOne() {
        byte[] input = GeneratedInput.binary(40_000).bytes();
        CompressionStats sequential = new Compressor(settings(TripletCoding.VALACH)).compressWithStats(input).stats();

        try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            CompressionStats parallel = new Compressor(settings(TripletCoding.VALACH), ConfiguredACBProvider::new,
                    OrderedMapper.on(pool, THREADS)).compressWithStats(input).stats();

            assertThat(parallel).isEqualTo(sequential);
        }
    }

    @Test
    void segmentsAreCodedAndDecodedOnThePoolNotOnTheCallingThread() throws MalformedStreamException {
        byte[] input = GeneratedInput.text(30_000).bytes();
        ThreadRecordingProvider provider = ThreadRecordingProvider.of(settings(TripletCoding.VALACH));

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Compressor compressor = new Compressor(settings(TripletCoding.VALACH), provider.components(),
                    OrderedMapper.on(pool, 4));

            compressor.decompress(compressor.compress(input));
        }

        assertThat(provider.threads()).isNotEmpty().doesNotContain(Thread.currentThread());
    }

    @Test
    void aBlockThatCannotBeDecodedFailsTheWholeDecompressionOnAPool() {
        CompressionSettings settings = settings(TripletCoding.VALACH);
        CompressedStream sound = new Compressor(settings).compress(GeneratedInput.text(20_000).bytes());
        List<Block> blocks = new ArrayList<>(sound.blocks());
        blocks.set(1, Block.coded(blocks.get(1).rawLength(), new byte[]{1}));
        CompressedStream damaged = new CompressedStream(sound.header(), blocks);

        try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            Compressor parallel = new Compressor(settings, ConfiguredACBProvider::new, OrderedMapper.on(pool, 8));

            assertThatThrownBy(() -> parallel.decompress(damaged)).isInstanceOf(MalformedStreamException.class);
        }
    }

    @Test
    void aSegmentOfTheWrongSizeIsRejectedOnAPoolToo() {
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Compressor parallel = new Compressor(settings(TripletCoding.VALACH), ConfiguredACBProvider::new,
                    OrderedMapper.on(pool, 4));

            assertThatThrownBy(() -> parallel.compress(List.of(new byte[10], new byte[SEGMENT_SIZE + 1]).iterator()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
