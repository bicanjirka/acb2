package cz.cvut.fit.acb;

import cz.cvut.fit.acb.coding.TripletWriter;
import cz.cvut.fit.acb.dictionary.ByteArray;
import cz.cvut.fit.acb.dictionary.ByteBuilder;
import cz.cvut.fit.acb.format.CompressedStream;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.format.StreamHeader;
import cz.cvut.fit.acb.triplets.TripletProcessor;
import cz.cvut.fit.acb.triplets.coder.TripletCoder;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Compresses bytes into a {@link CompressedStream} and back, in memory. Holds no state between
 * calls, so one instance serves any number of streams. Each segment is coded with a dictionary of
 * its own; decompression takes the coding settings from the stream's header and only the
 * dictionary structure from this instance.
 */
public final class Compressor {

    private final CompressionSettings settings;
    private final Function<CompressionSettings, ACBProvider> components;

    public Compressor(CompressionSettings settings) {
        this(settings, ACBProviderImpl::new);
    }

    public Compressor(CompressionSettings settings, Function<CompressionSettings, ACBProvider> components) {
        this.settings = settings;
        this.components = components;
    }

    public CompressedStream compress(byte[] input) {
        return this.compress(segmentsOf(input, this.settings.segmentSize()));
    }

    /**
     * Every segment but the last must have the first one's size: the stream records only that size,
     * and the decoder splits by it.
     */
    public CompressedStream compress(Iterator<byte[]> segments) {
        ACBProvider provider = this.components.apply(this.settings);
        TripletWriter writer = provider.getTripletWriter();
        boolean sizeAnnounced = false;
        while (segments.hasNext()) {
            byte[] segment = segments.next();
            if (!sizeAnnounced) {
                writer.setSize(segment.length);
                sizeAnnounced = true;
            }
            ByteArray sequence = new ByteArray(segment);
            provider.getCoder(sequence, provider.getDictionary(sequence)).encode(triplet -> triplet.visit(writer));
        }
        if (!sizeAnnounced) {
            writer.setSize(0);
        }
        return new CompressedStream(StreamHeader.of(this.settings), writer.finish());
    }

    public byte[] decompress(CompressedStream stream) throws MalformedStreamException {
        ByteBuilder decoded = new ByteBuilder();
        this.decompress(stream, decoded::append);
        return decoded.array();
    }

    /** Hands each decoded segment to {@code segments} as soon as it is complete. */
    public void decompress(CompressedStream stream, Consumer<byte[]> segments) throws MalformedStreamException {
        List<byte[]> payload = stream.payload();
        if (payload.isEmpty() || payload.getFirst().length != Integer.BYTES
                || ByteBuffer.wrap(payload.getFirst()).getInt() < 0) {
            throw new MalformedStreamException("ACB payload does not start with a segment size");
        }
        ACBProvider provider = this.components.apply(stream.header().toSettings(this.settings.dictionaryStructure()));
        TripletProcessor reader = provider.getTripletReader(payload);
        TripletCoder.DecodeFlag flag;
        do {
            ByteBuilder segment = new ByteBuilder();
            flag = provider.getCoder(segment, provider.getDictionary(segment)).decode(reader);
            if (segment.length() > 0) {
                segments.accept(segment.array());
            }
        } while (flag != TripletCoder.DecodeFlag.EOF);
    }

    private static Iterator<byte[]> segmentsOf(byte[] input, int segmentSize) {
        return new Iterator<>() {
            private long from;

            @Override
            public boolean hasNext() {
                return this.from < input.length;
            }

            @Override
            public byte[] next() {
                if (!this.hasNext()) {
                    throw new NoSuchElementException();
                }
                int to = (int) Math.min(input.length, this.from + segmentSize);
                byte[] segment = Arrays.copyOfRange(input, (int) this.from, to);
                this.from = to;
                return segment;
            }
        };
    }
}
