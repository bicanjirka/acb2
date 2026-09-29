package cz.cvut.fit.acb.triplets.coder;

import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.dictionary.DictionaryInfo;
import cz.cvut.fit.acb.dictionary.SegmentBuffer;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.triplets.TripletProcessor;
import cz.cvut.fit.acb.triplets.TripletSupplier;
import cz.cvut.fit.acb.utils.BitUtils;

import java.util.function.Consumer;

/**
 * The encode and decode loops over one segment; each coder supplies one step, which lays out a
 * triplet's fields.
 *
 */
public abstract sealed class BaseTripletCoder implements TripletCoder
        permits SimpleTripletCoder, SalomonTripletCoder, ValachTripletCoder {

    private final Dictionary dictionary;
    private final SegmentBuffer segment;
    private final int distanceBits;

    protected BaseTripletCoder(SegmentBuffer segment, Dictionary dictionary, int distanceBits) {
        this.segment = segment;
        this.dictionary = dictionary;
        this.distanceBits = distanceBits;
    }

    @Override
    public void encode(Consumer<TripletSupplier> output) {
        int idx = 0;
        int ceiling = this.segment.length();
        while (idx < ceiling) {
            DictionaryInfo info = this.dictionary.search(idx);
            idx = this.encodeStep(idx, info, output);
        }
    }

    @Override
    public DecodeFlag decode(TripletProcessor input) throws MalformedStreamException {
        int ceiling = input.getSize();
        if (ceiling == 0) {
            return DecodeFlag.EOF;
        }
        int idx = 0;
        while (idx < ceiling) {
            idx = this.decodeStep(idx, input);
        }
        if (idx == ceiling) {
            return DecodeFlag.END_OF_PARTITION;
        }
        if (idx == Integer.MAX_VALUE) {
            return DecodeFlag.EOF;
        }
        throw new MalformedStreamException("A triplet runs " + (idx - ceiling) + " bytes past the end of its segment");
    }

    protected abstract int encodeStep(int idx, DictionaryInfo info, Consumer<TripletSupplier> output);

    /**
     * @return the index after the decoded triplet, or {@link Integer#MAX_VALUE} if the stream ends
     *         before a triplet
     * @throws MalformedStreamException if the stream ends inside a triplet or a triplet is unsound
     */
    protected abstract int decodeStep(int idx, TripletProcessor input) throws MalformedStreamException;

    /** A field that must be there once its triplet has begun. */
    protected static int requireField(int value) throws MalformedStreamException {
        if (value == -1) {
            throw new MalformedStreamException("The stream ends inside a triplet");
        }
        return value;
    }

    protected final Dictionary dictionary() {
        return this.dictionary;
    }

    protected final SegmentBuffer segment() {
        return this.segment;
    }

    /**
     * Appends the first {@code length} bytes of the content of rank {@code rank}, repeating them if
     * the content is shorter.
     *
     * @throws MalformedStreamException if the rank is not in the dictionary
     */
    protected final void appendContent(int rank, int length) throws MalformedStreamException {
        if (length > 0) {
            this.segment.appendCopy(this.dictionary.select(rank), length);
        }
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
