package cz.cvut.fit.acb.fixtures;

import cz.cvut.fit.acb.ACBProvider;
import cz.cvut.fit.acb.CompressionSettings;
import cz.cvut.fit.acb.ConfiguredACBProvider;
import cz.cvut.fit.acb.coding.TripletWriter;
import cz.cvut.fit.acb.dictionary.DecoderDictionary;
import cz.cvut.fit.acb.dictionary.EncoderDictionary;
import cz.cvut.fit.acb.dictionary.SegmentBuffer;
import cz.cvut.fit.acb.format.Block;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.triplets.FieldSource;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/** The configured provider, noting which threads asked it for the components of a segment. */
public final class ThreadRecordingProvider implements ACBProvider {

    private final ACBProvider provider;
    private final Set<Thread> threads = Collections.synchronizedSet(new HashSet<>());

    private ThreadRecordingProvider(ACBProvider provider) {
        this.provider = provider;
    }

    /** Components for a {@code Compressor}, and the recorder to ask afterwards. */
    public static ThreadRecordingProvider of(CompressionSettings settings) {
        return new ThreadRecordingProvider(new ConfiguredACBProvider(settings));
    }

    public Function<CompressionSettings, ACBProvider> components() {
        return settings -> this;
    }

    /** The threads that asked for a dictionary or a writer, in no order. */
    public Set<Thread> threads() {
        synchronized (this.threads) {
            return Set.copyOf(this.threads);
        }
    }

    @Override
    public EncoderDictionary encoderDictionary(SegmentBuffer segment) {
        this.threads.add(Thread.currentThread());
        return this.provider.encoderDictionary(segment);
    }

    @Override
    public DecoderDictionary decoderDictionary(SegmentBuffer segment) {
        this.threads.add(Thread.currentThread());
        return this.provider.decoderDictionary(segment);
    }

    @Override
    public TripletWriter writer() {
        return this.provider.writer();
    }

    @Override
    public FieldSource reader(byte[] block) throws MalformedStreamException {
        return this.provider.reader(block);
    }

    @Override
    public Block block(byte[] segment, byte[] coded) {
        return this.provider.block(segment, coded);
    }
}
