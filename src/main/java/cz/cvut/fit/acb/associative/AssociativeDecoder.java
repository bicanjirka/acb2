package cz.cvut.fit.acb.associative;

import cz.cvut.fit.acb.coding.RangeDecoder;
import cz.cvut.fit.acb.dictionary.AnalogyDictionary;
import cz.cvut.fit.acb.dictionary.SegmentBuffer;
import cz.cvut.fit.acb.format.MalformedStreamException;

/** Rebuilds one segment that {@link AssociativeEncoder} coded; used once. */
public final class AssociativeDecoder {

    private final AssociativeVariant variant;
    private final AnalogyDictionary dictionary;
    private final SegmentBuffer segment;
    private final int reach;
    private final int maxLength;
    private final int[] lengthStart;

    /**
     * @param dictionary a dictionary over {@code segment}, which must be empty and which this appends to
     * @param reach the most candidates a funnel takes from either side of a context
     * @param maxLength the longest match, which is cut short there
     * @param lengthStart where the model of the lengths starts from, one frequency per excess
     */
    public AssociativeDecoder(AssociativeVariant variant, AnalogyDictionary dictionary, SegmentBuffer segment,
                              int reach, int maxLength, int[] lengthStart) {
        this.variant = variant;
        this.dictionary = dictionary;
        this.segment = segment;
        this.reach = reach;
        this.maxLength = maxLength;
        this.lengthStart = lengthStart;
    }

    /**
     * Decodes {@code block} into the segment until it holds {@code length} bytes.
     *
     * @throws MalformedStreamException if the block is too short, or asks for a step no text can have
     */
    public void decode(byte[] block, int length) throws MalformedStreamException {
        StepRule steps = this.variant.steps(this.dictionary, this.segment, length, this.maxLength, this.lengthStart,
                2 * this.reach, new DecodingPort(this.segment, new RangeDecoder(block)));
        int idx = 0;
        while (idx < length) {
            int coded = steps.step(idx);
            this.dictionary.update(idx, coded);
            idx += coded;
        }
    }
}
