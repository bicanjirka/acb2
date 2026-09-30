package cz.cvut.fit.acb.associative;

import cz.cvut.fit.acb.dictionary.AnalogyDictionary;
import cz.cvut.fit.acb.dictionary.SegmentBuffer;

/** Which step rule an associative coder runs: Buyanovsky's own, or the variant that mixes its models. */
public enum AssociativeVariant {

    /** {@code AC.C}'s coding of a step (ALGORITHM.md section 7). */
    BUYANOVSKY {
        @Override
        StepRule steps(AnalogyDictionary dictionary, SegmentBuffer text, int segmentLength, int maxLength,
                       int[] lengthStart, int funnelCapacity, StepPort port) {
            return new AssociativeSteps(dictionary, text, segmentLength, maxLength, lengthStart, funnelCapacity, port);
        }
    },

    /** The same funnels and matches, with the models of a step mixed (ALGORITHM.md section 9). */
    MIXED {
        @Override
        StepRule steps(AnalogyDictionary dictionary, SegmentBuffer text, int segmentLength, int maxLength,
                       int[] lengthStart, int funnelCapacity, StepPort port) {
            return new MixedSteps(dictionary, text, segmentLength, maxLength, funnelCapacity, port);
        }
    };

    /**
     * @param text the bytes known so far: all of them for the encoder, those decoded for the decoder
     * @param maxLength the longest match, which is cut short there
     * @param lengthStart where the model of the lengths starts from, one frequency per excess
     * @param funnelCapacity the most candidates a funnel holds
     */
    abstract StepRule steps(AnalogyDictionary dictionary, SegmentBuffer text, int segmentLength, int maxLength,
                            int[] lengthStart, int funnelCapacity, StepPort port);
}
