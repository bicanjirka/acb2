package cz.cvut.fit.acb.associative;

import cz.cvut.fit.acb.coding.CumulativeTable;
import cz.cvut.fit.acb.dictionary.AnalogyDictionary;
import cz.cvut.fit.acb.dictionary.Funnel;
import cz.cvut.fit.acb.dictionary.SegmentBuffer;
import cz.cvut.fit.acb.format.MalformedStreamException;

/**
 * One step of the associative coder, the same on both sides, with the symbols of it coming from a
 * {@link SymbolPort}. A step gathers the funnel of analogies of the context and codes which candidate
 * continues the text, or none. The match is coded as what it has beyond the longest match an earlier
 * candidate has, which the decoder finds as their longest common prefix with the chosen content. When
 * the match ended on a mismatch, a literal follows, which cannot be a byte that a candidate matching as
 * long would have gone on with, and is voted on by the funnel of the context the match made.
 */
final class AssociativeSteps implements StepRule {

    private final AnalogyDictionary dictionary;
    private final SegmentBuffer text;
    private final int segmentLength;
    private final int maxLength;
    private final SymbolPort port;
    private final PositionModel positions = new PositionModel();
    private final LengthModel lengths;
    private final ForecastedLiterals literals = new ForecastedLiterals();
    private final CumulativeTable table = new CumulativeTable();
    private final int[] common;

    /**
     * @param text the bytes known so far: all of them for the encoder, those decoded for the decoder
     * @param maxLength the longest match, which is cut short there
     * @param lengthStart where the model of the lengths starts from, one frequency per excess
     * @param funnelCapacity the most candidates a funnel holds
     */
    AssociativeSteps(AnalogyDictionary dictionary, SegmentBuffer text, int segmentLength, int maxLength,
                     int[] lengthStart, int funnelCapacity, SymbolPort port) {
        this.dictionary = dictionary;
        this.text = text;
        this.segmentLength = segmentLength;
        this.maxLength = maxLength;
        this.lengths = new LengthModel(lengthStart);
        this.common = new int[funnelCapacity];
        this.port = port;
    }

    @Override
    public int step(int idx) throws MalformedStreamException {
        Funnel funnel = this.dictionary.funnel(idx);
        this.literals.reset();
        if (funnel.size() == 0) {
            this.literal(idx);
            return 1;
        }
        this.positions.fill(funnel, this.table);
        int choice = this.port.position(idx, funnel, this.table);
        this.positions.record(funnel, choice == 0);
        if (choice == 0) {
            for (int i = 0; i < funnel.size(); i++) {
                this.literals.exclude(this.byteAt(funnel.position(i)));
            }
            this.literal(idx);
            return 1;
        }
        return this.match(idx, funnel, choice - 1);
    }

    private int match(int idx, Funnel funnel, int chosen) throws MalformedStreamException {
        int content = funnel.position(chosen);
        int cap = Math.min(this.maxLength, idx - content);
        int known = this.commonWithChosen(idx, funnel, chosen);
        if (cap - known < 1) {
            throw new MalformedStreamException("A candidate cannot match more than the ones before it do");
        }
        for (int i = chosen + 1; i < funnel.size(); i++) {
            if (this.common[i] > known && this.common[i] <= cap) {
                this.lengths.share(this.common[i] - known - 1);
            }
        }
        this.lengths.fill(this.table, cap - known);
        int excess = this.port.length(this.table, known);
        this.lengths.update(excess);
        int length = known + 1 + excess;
        if (idx + length > this.segmentLength) {
            throw new MalformedStreamException("A match runs past the end of its segment");
        }
        this.port.copied(content, length);
        if (length >= this.maxLength || idx + length >= this.segmentLength
                || !this.excludeContinuations(idx, funnel, chosen, length)) {
            return length;
        }
        Funnel forecast = this.dictionary.forecast(idx + length);
        for (int i = 0; i < forecast.size(); i++) {
            this.literals.vote(this.byteAt(forecast.position(i)), forecast.weight(i));
        }
        this.literal(idx + length);
        return length + 1;
    }

    /**
     * Finds how many bytes each candidate shares with the chosen content, over the bytes before
     * {@code idx}, which both sides have.
     *
     * @return the most the candidates before the chosen one share with it
     */
    private int commonWithChosen(int idx, Funnel funnel, int chosen) {
        int content = funnel.position(chosen);
        int known = 0;
        for (int i = 0; i < funnel.size(); i++) {
            if (i != chosen) {
                int other = funnel.position(i);
                this.common[i] = this.text.commonLength(other, content, idx - Math.max(other, content));
                if (i < chosen && this.common[i] > known) {
                    known = this.common[i];
                }
            }
        }
        return known;
    }

    /**
     * Excludes the byte each candidate that matched as far as the chosen one goes on with, since it
     * would have made the match longer.
     *
     * @return whether some byte is excluded; if not, nothing says the match ended on a mismatch
     */
    private boolean excludeContinuations(int idx, Funnel funnel, int chosen, int length) {
        boolean any = false;
        for (int i = 0; i < funnel.size(); i++) {
            int next = funnel.position(i) + length;
            if ((i == chosen || this.common[i] >= length) && next < idx) {
                this.literals.exclude(this.byteAt(next));
                any = true;
            }
        }
        return any;
    }

    private void literal(int at) throws MalformedStreamException {
        if (!this.literals.fill(this.table)) {
            throw new MalformedStreamException("No byte can follow the match");
        }
        this.literals.update(this.port.literal(at, this.table));
    }

    private int byteAt(int index) {
        return Byte.toUnsignedInt(this.text.byteAt(index));
    }
}
