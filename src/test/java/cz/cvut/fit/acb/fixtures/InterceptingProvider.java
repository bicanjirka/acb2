package cz.cvut.fit.acb.fixtures;

import cz.cvut.fit.acb.ACBProvider;
import cz.cvut.fit.acb.CompressionSettings;
import cz.cvut.fit.acb.ConfiguredACBProvider;
import cz.cvut.fit.acb.coding.TripletWriter;
import cz.cvut.fit.acb.dictionary.AnalogyDictionary;
import cz.cvut.fit.acb.dictionary.DecoderDictionary;
import cz.cvut.fit.acb.dictionary.EncoderDictionary;
import cz.cvut.fit.acb.dictionary.SegmentBuffer;
import cz.cvut.fit.acb.format.Block;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.triplets.FieldSource;

import java.util.function.Function;

/**
 * The real components, with each dictionary passed through {@code snapshots} and every field
 * through {@code log}, and every block coded.
 */
public final class InterceptingProvider implements ACBProvider {

    private final ACBProvider delegate;
    private final DictionarySnapshots.Side snapshots;
    private final TripletLog log;

    public InterceptingProvider(ACBProvider delegate, DictionarySnapshots.Side snapshots, TripletLog log) {
        this.delegate = delegate;
        this.snapshots = snapshots;
        this.log = log;
    }

    /** For {@code new Compressor(settings, components(...))}. */
    public static Function<CompressionSettings, ACBProvider> components(DictionarySnapshots.Side snapshots,
                                                                       TripletLog log) {
        return settings -> new InterceptingProvider(new ConfiguredACBProvider(settings), snapshots, log);
    }

    /** Intercepts only the fields, leaving the dictionaries alone. */
    public static Function<CompressionSettings, ACBProvider> logging(TripletLog log) {
        return components(DictionarySnapshots.Side.ignoring(), log);
    }

    @Override
    public EncoderDictionary encoderDictionary(SegmentBuffer segment) {
        return this.snapshots.observing(this.delegate.encoderDictionary(segment));
    }

    @Override
    public DecoderDictionary decoderDictionary(SegmentBuffer segment) {
        return this.snapshots.observing(this.delegate.decoderDictionary(segment));
    }

    @Override
    public AnalogyDictionary analogyDictionary(SegmentBuffer segment) {
        return this.snapshots.observing(this.delegate.analogyDictionary(segment));
    }

    @Override
    public TripletWriter writer() {
        return this.log.recording(this.delegate.writer());
    }

    @Override
    public FieldSource reader(byte[] block) throws MalformedStreamException {
        return this.log.checking(this.delegate.reader(block), block);
    }

    /** Always codes, so that the decoder is exercised however little coding gains on a small segment. */
    @Override
    public Block block(byte[] segment, byte[] coded) {
        return Block.coded(segment.length, coded);
    }
}
