package cz.cvut.fit.acb.triplets;

import cz.cvut.fit.acb.dictionary.Dictionary;

/**
 * Where coding a segment stands: the position it continues from, and the dictionary of what came
 * before. Both sides advance it only through {@link #apply}, so the encoder and the decoder cannot
 * disagree about what a triplet does to the dictionary.
 */
public final class SegmentState {

    private final Dictionary dictionary;
    private int position;

    public SegmentState(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    /** The position coding continues from. */
    public int position() {
        return this.position;
    }

    /** Adds the bytes the triplet coded, which the segment must already hold, to the dictionary. */
    public void apply(Triplet triplet) {
        int consumed = triplet.consumed();
        this.dictionary.update(this.position, consumed);
        this.position += consumed;
    }
}
