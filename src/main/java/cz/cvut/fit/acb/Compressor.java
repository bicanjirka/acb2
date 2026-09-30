package cz.cvut.fit.acb;

import cz.cvut.fit.acb.format.Block;
import cz.cvut.fit.acb.format.BlockSource;
import cz.cvut.fit.acb.format.CompressedStream;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.format.StreamHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Compresses bytes into a {@link CompressedStream} and back, in memory, or a block at a time
 * through {@link #compressInto} and {@link #decompress(StreamHeader, BlockSource, Consumer)}.
 * Holds no state between calls, so one instance serves any number of streams. Every segment becomes a block of its own,
 * coded with a dictionary and entropy models that start empty, and stored as it is when coding
 * would not shrink it; decompression takes the coding settings from the stream's header. Blocks
 * depend on nothing before them, so an {@link OrderedMapper} on an executor may code or decode
 * several at once; the result is the same as one after the other.
 */
public final class Compressor {

    private static final Logger LOG = LogManager.getLogger();

    private final CompressionSettings settings;
    private final Function<CompressionSettings, ACBProvider> components;
    private final OrderedMapper mapper;

    public Compressor(CompressionSettings settings) {
        this(settings, ConfiguredACBProvider::new);
    }

    public Compressor(CompressionSettings settings, Function<CompressionSettings, ACBProvider> components) {
        this(settings, components, OrderedMapper.sequential());
    }

    /** A mapper on an executor calls the provider from several threads, so it must be safe to. */
    public Compressor(CompressionSettings settings, Function<CompressionSettings, ACBProvider> components,
                      OrderedMapper mapper) {
        this.settings = settings;
        this.components = components;
        this.mapper = mapper;
    }

    public CompressedStream compress(byte[] input) {
        return this.compress(new ByteSegments(input, this.settings.segmentSize()));
    }

    /** @throws IllegalArgumentException if a segment is empty or longer than the segment size of the settings */
    public CompressedStream compress(Iterator<byte[]> segments) {
        return this.compressWithStats(segments).stream();
    }

    public CompressionResult compressWithStats(byte[] input) {
        return this.compressWithStats(new ByteSegments(input, this.settings.segmentSize()));
    }

    /** @throws IllegalArgumentException if a segment is empty or longer than the segment size of the settings */
    public CompressionResult compressWithStats(Iterator<byte[]> segments) {
        List<Block> blocks = new ArrayList<>();
        CompressionStats stats = this.compressInto(segments, blocks::add);
        return new CompressionResult(new CompressedStream(StreamHeader.of(this.settings), blocks), stats);
    }

    /**
     * Hands each segment's block to {@code blocks} in order as soon as it is coded, so a caller can
     * write them out without holding them all.
     *
     * @throws IllegalArgumentException if a segment is empty or longer than the segment size of the settings
     */
    public CompressionStats compressInto(Iterator<byte[]> segments, Consumer<Block> blocks) {
        ACBProvider provider = this.components.apply(this.settings);
        SegmentCoding coding = this.settings.tripletCoding().segmentCoding(this.settings);
        TotalledBlocks totalled = new TotalledBlocks(blocks);
        this.mapper.map(OrderedMapper.Feed.of(segments),
                segment -> coding.encode(requireSegmentSize(segment, this.settings.segmentSize()), provider),
                totalled);
        CompressionStats stats = totalled.stats;
        LOG.debug("Compressed {} bytes in {} segments into {} triplets, {} bits of fields", stats.inputBytes(),
                stats.segments(), stats.triplets(), stats.fieldBits());
        return stats;
    }

    public byte[] decompress(CompressedStream stream) throws MalformedStreamException {
        ByteArrayOutputStream decoded = new ByteArrayOutputStream();
        this.decompress(stream, decoded::writeBytes);
        return decoded.toByteArray();
    }

    /** Hands each decoded segment to {@code segments} as soon as it is complete. */
    public void decompress(CompressedStream stream, Consumer<byte[]> segments) throws MalformedStreamException {
        this.decompress(stream.header(), BlockSource.of(stream.blocks()), segments);
    }

    /**
     * Decodes blocks as they are read from {@code blocks} and hands each segment to {@code segments}
     * in order. A stream that damages later on fails only then, after earlier segments went out, so
     * a caller keeps what it wrote only once this returns.
     */
    public void decompress(StreamHeader header, BlockSource blocks, Consumer<byte[]> segments)
            throws MalformedStreamException {
        CompressionSettings settings = header.toSettings();
        ACBProvider provider = this.components.apply(settings);
        SegmentCoding coding = settings.tripletCoding().segmentCoding(settings);
        AtomicLong bytes = new AtomicLong();
        this.mapper.map(blocks::next, block -> switch (block) {
            case Block.Stored stored -> stored.bytes();
            case Block.Coded coded -> coding.decode(coded, provider);
        }, segment -> {
            bytes.addAndGet(segment.length);
            segments.accept(segment);
        });
        LOG.debug("Decompressed {} bytes", bytes.get());
    }

    private static byte[] requireSegmentSize(byte[] segment, int segmentSize) {
        if (segment.length < 1 || segment.length > segmentSize) {
            throw new IllegalArgumentException("A segment holds 1 to " + segmentSize + " bytes, not " + segment.length);
        }
        return segment;
    }

    /** Passes blocks on and adds up what coding them did; only the caller's thread uses it. */
    private static final class TotalledBlocks implements Consumer<CodedSegment> {

        private final Consumer<Block> blocks;
        private CompressionStats stats = CompressionStats.none();

        private TotalledBlocks(Consumer<Block> blocks) {
            this.blocks = blocks;
        }

        @Override
        public void accept(CodedSegment segment) {
            this.blocks.accept(segment.block());
            this.stats = this.stats.plus(segment.stats());
        }
    }
}
