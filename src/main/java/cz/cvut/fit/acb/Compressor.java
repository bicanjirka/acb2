package cz.cvut.fit.acb;

import cz.cvut.fit.acb.coding.TripletWriter;
import cz.cvut.fit.acb.dictionary.SearchWindow;
import cz.cvut.fit.acb.dictionary.SegmentBuffer;
import cz.cvut.fit.acb.format.Block;
import cz.cvut.fit.acb.format.CompressedStream;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.format.StreamHeader;
import cz.cvut.fit.acb.triplets.FieldSource;
import cz.cvut.fit.acb.triplets.SegmentDecoder;
import cz.cvut.fit.acb.triplets.SegmentEncoder;
import cz.cvut.fit.acb.triplets.TripletLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.StreamSupport;

/**
 * Compresses bytes into a {@link CompressedStream} and back, in memory. Holds no state between
 * calls, so one instance serves any number of streams. Every segment becomes a block of its own,
 * coded with a dictionary and entropy models that start empty, and stored as it is when coding
 * would not shrink it; decompression takes the coding settings from the stream's header.
 */
public final class Compressor {

    private static final Logger LOG = LogManager.getLogger();

    private final CompressionSettings settings;
    private final Function<CompressionSettings, ACBProvider> components;

    public Compressor(CompressionSettings settings) {
        this(settings, ConfiguredACBProvider::new);
    }

    public Compressor(CompressionSettings settings, Function<CompressionSettings, ACBProvider> components) {
        this.settings = settings;
        this.components = components;
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
        ACBProvider provider = this.components.apply(this.settings);
        TripletLayout layout = this.settings.tripletCoding().layout(this.settings.distanceBits(),
                this.settings.lengthBits());
        List<CodedSegment> coded = StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(segments, Spliterator.ORDERED), false)
                .map(segment -> this.code(requireSegmentSize(segment, this.settings.segmentSize()), provider, layout))
                .toList();
        CompressionStats stats = coded.stream().map(CodedSegment::stats).reduce(CompressionStats.none(),
                CompressionStats::plus);
        CompressedStream stream = new CompressedStream(StreamHeader.of(this.settings),
                coded.stream().map(CodedSegment::block).toList());
        LOG.debug("Compressed {} bytes in {} segments into {} triplets, {} bits of fields", stats.inputBytes(),
                stats.segments(), stats.triplets(), stats.fieldBits());
        return new CompressionResult(stream, stats);
    }

    private CodedSegment code(byte[] segment, ACBProvider provider, TripletLayout layout) {
        SegmentBuffer buffer = SegmentBuffer.of(segment);
        TripletWriter writer = provider.writer();
        long triplets = new SegmentEncoder(provider.encoderDictionary(buffer), this.settings.tripletCoding().parser(),
                layout, this.settings.searchWindow()).encode(buffer, writer);
        Block block = provider.block(segment, writer.finish());
        return new CodedSegment(block, new CompressionStats(segment.length, 1, triplets, writer.costs()));
    }

    public byte[] decompress(CompressedStream stream) throws MalformedStreamException {
        ByteArrayOutputStream decoded = new ByteArrayOutputStream();
        this.decompress(stream, decoded::writeBytes);
        return decoded.toByteArray();
    }

    /** Hands each decoded segment to {@code segments} as soon as it is complete. */
    public void decompress(CompressedStream stream, Consumer<byte[]> segments) throws MalformedStreamException {
        CompressionSettings settings = stream.header().toSettings();
        ACBProvider provider = this.components.apply(settings);
        TripletLayout layout = settings.tripletCoding().layout(settings.distanceBits(), settings.lengthBits());
        long bytes = 0;
        for (Block block : stream.blocks()) {
            segments.accept(switch (block) {
                case Block.Stored stored -> stored.bytes();
                case Block.Coded coded -> decode(provider, layout, settings.searchWindow(), coded);
            });
            bytes += block.rawLength();
        }
        LOG.debug("Decompressed {} bytes", bytes);
    }

    private static byte[] decode(ACBProvider provider, TripletLayout layout, SearchWindow window,
                                 Block.Coded block)
            throws MalformedStreamException {
        SegmentBuffer segment = SegmentBuffer.empty();
        FieldSource source = provider.reader(block.bytes());
        new SegmentDecoder(provider.decoderDictionary(segment), layout, window).decode(segment, block.rawLength(), source);
        return segment.toArray();
    }

    private static byte[] requireSegmentSize(byte[] segment, int segmentSize) {
        if (segment.length < 1 || segment.length > segmentSize) {
            throw new IllegalArgumentException("A segment holds 1 to " + segmentSize + " bytes, not " + segment.length);
        }
        return segment;
    }

    /** One segment as coded, and what coding it did. */
    private record CodedSegment(Block block, CompressionStats stats) {
    }
}
