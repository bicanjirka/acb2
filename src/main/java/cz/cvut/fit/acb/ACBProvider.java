package cz.cvut.fit.acb;

import cz.cvut.fit.acb.coding.TripletWriter;
import cz.cvut.fit.acb.dictionary.AnalogyDictionary;
import cz.cvut.fit.acb.dictionary.DecoderDictionary;
import cz.cvut.fit.acb.dictionary.EncoderDictionary;
import cz.cvut.fit.acb.dictionary.SegmentBuffer;
import cz.cvut.fit.acb.format.Block;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.triplets.FieldSource;

/**
 * Builds the stateful components one segment is coded with, so tests can observe them.
 * {@link Compressor} asks for fresh ones per segment; implementations hold no state of their own.
 */
public interface ACBProvider {

    EncoderDictionary encoderDictionary(SegmentBuffer segment);

    DecoderDictionary decoderDictionary(SegmentBuffer segment);

    /** The dictionary of the associative coder, which is the same on both sides. */
    AnalogyDictionary analogyDictionary(SegmentBuffer segment);

    TripletWriter writer();

    /** @throws MalformedStreamException if the block is too short to start reading */
    FieldSource reader(byte[] block) throws MalformedStreamException;

    /** Chooses how a segment is stored, given the bytes coding it took. */
    Block block(byte[] segment, byte[] coded);
}
