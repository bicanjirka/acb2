package cz.cvut.fit.acb.triplets.coder;

import cz.cvut.fit.acb.dictionary.ByteSequence;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.dictionary.DictionaryInfo;
import cz.cvut.fit.acb.triplets.TripletProcessor;
import cz.cvut.fit.acb.triplets.TripletSupplier;
import cz.cvut.fit.acb.utils.BitUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

/**
 * The encode and decode loops over one segment; each coder supplies one step, which lays out a
 * triplet's fields.
 *
 */
public abstract sealed class BaseTripletCoder implements TripletCoder
        permits SimpleTripletCoder, SalomonTripletCoder, ValachTripletCoder, LCPTripletCoder {

    private static final Logger LOG = LogManager.getLogger();

    private final Dictionary dictionary;
    private final ByteSequence sequence;
    private final int distanceBits;

    protected BaseTripletCoder(ByteSequence sequence, Dictionary dictionary, int distanceBits) {
        this.sequence = sequence;
        this.dictionary = dictionary;
        this.distanceBits = distanceBits;
    }

    @Override
    public void encode(Consumer<TripletSupplier> output) {
        int idx = 0;
        int triplets = 0;
        int ceiling = this.sequence.length();
        while (idx < ceiling) {
            DictionaryInfo info = this.dictionary.search(idx);
            idx = this.encodeStep(idx, info, output);
            triplets++;
        }
        LOG.debug("Triplets in segment: {}", triplets);
    }

    @Override
    public DecodeFlag decode(TripletProcessor input) {
        int ceiling = input.getSize();
        if (ceiling == 0) {
            return DecodeFlag.EOF;
        }
        int idx = 0;
        int triplets = 0;
        while (idx < ceiling) {
            idx = this.decodeStep(idx, input);
            triplets++;
        }
        if (idx == ceiling) {
            LOG.debug("Triplets in segment: {}", triplets);
            return DecodeFlag.END_OF_PARTITION;
        }
        LOG.debug("Triplets in last segment: {}", triplets - 1);
        return DecodeFlag.EOF;
    }

    protected abstract int encodeStep(int idx, DictionaryInfo info, Consumer<TripletSupplier> output);

    /** @return the index after the decoded triplet, or {@link Integer#MAX_VALUE} at end of stream */
    protected abstract int decodeStep(int idx, TripletProcessor input);

    protected final Dictionary dictionary() {
        return this.dictionary;
    }

    protected final ByteSequence sequence() {
        return this.sequence;
    }

    /** Keeps a signed distance's low bits, as the distance field stores it. */
    protected final int distanceMask() {
        return (1 << this.distanceBits) - 1;
    }

    /** Undoes {@link #distanceMask()}: sign-extends a stored distance field. */
    protected final int signedDistance(int stored) {
        return BitUtils.isNegative(stored, this.distanceBits) ? BitUtils.fillHighBits(stored) : stored;
    }
}
