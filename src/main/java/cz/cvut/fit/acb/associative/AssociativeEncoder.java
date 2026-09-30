package cz.cvut.fit.acb.associative;

import cz.cvut.fit.acb.coding.FieldCost;
import cz.cvut.fit.acb.dictionary.AnalogyDictionary;
import cz.cvut.fit.acb.dictionary.SegmentBuffer;
import cz.cvut.fit.acb.format.MalformedStreamException;

import java.util.List;

/** Codes one whole segment with Buyanovsky's associative coder; used once. */
public final class AssociativeEncoder {

    private final AnalogyDictionary dictionary;
    private final SegmentBuffer segment;
    private final EncodingPort port;
    private final AssociativeSteps steps;

    /**
     * @param dictionary a dictionary over {@code segment}, which it reads but never changes
     * @param reach the most candidates a funnel takes from either side of a context
     * @param maxLength the longest match, which is cut short there
     * @param lengthStart where the model of the lengths starts from, one frequency per excess
     */
    public AssociativeEncoder(AnalogyDictionary dictionary, SegmentBuffer segment, int reach, int maxLength,
                              int[] lengthStart) {
        this.dictionary = dictionary;
        this.segment = segment;
        this.port = new EncodingPort(segment, maxLength);
        this.steps = new AssociativeSteps(dictionary, segment, segment.length(), maxLength, lengthStart, 2 * reach,
                this.port);
    }

    /** @return how many steps the segment took */
    public long encode() {
        long count = 0;
        int idx = 0;
        while (idx < this.segment.length()) {
            int coded;
            try {
                coded = this.steps.step(idx);
            } catch (MalformedStreamException e) {
                throw new IllegalStateException("The encoder made a step its own rules reject", e);
            }
            this.dictionary.update(idx, coded);
            idx += coded;
            count++;
        }
        return count;
    }

    /** The coded bytes; call after {@link #encode}. */
    public byte[] finish() {
        return this.port.finish();
    }

    /** What each kind of field cost; only meaningful after {@link #finish}. */
    public List<FieldCost> costs() {
        return this.port.costs();
    }
}
