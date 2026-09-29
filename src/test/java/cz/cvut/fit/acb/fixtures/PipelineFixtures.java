package cz.cvut.fit.acb.fixtures;

import cz.cvut.fit.acb.CompressionSettings;
import cz.cvut.fit.acb.Compressor;
import cz.cvut.fit.acb.format.MalformedStreamException;


public final class PipelineFixtures {

    private PipelineFixtures() {
    }

    /** Compresses and decompresses in memory, checking every triplet field on the way back. */
    public static byte[] roundTrip(CompressionSettings settings, byte[] input) {
        Compressor compressor = new Compressor(settings,
                InterceptingProvider.logging(new TripletLog()));
        try {
            return compressor.decompress(compressor.compress(input));
        } catch (MalformedStreamException e) {
            throw new AssertionError("A stream just compressed must decode", e);
        }
    }
}
