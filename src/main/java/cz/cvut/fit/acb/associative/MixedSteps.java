package cz.cvut.fit.acb.associative;

import cz.cvut.fit.acb.coding.RangeEncoder;
import cz.cvut.fit.acb.dictionary.AnalogyDictionary;
import cz.cvut.fit.acb.dictionary.Funnel;
import cz.cvut.fit.acb.dictionary.SegmentBuffer;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.mixing.Logistic;
import cz.cvut.fit.acb.triplets.TripletFieldKind;

/**
 * One step of the mixed associative coder, the same on both sides. A step gathers the funnel of
 * analogies of the context, adds the repeats of the last step, and walks down the contents of these
 * candidates a byte at a time, as the funnel of posthistory of Buyanovsky's paper: at each byte it codes
 * whether the text leaves all the candidates still alive, and if not, which of the bytes they go on with
 * it takes. The bytes the text shares with the candidates are the match, copied from any of those left;
 * where it leaves them all, a literal follows, which cannot be a byte any of them went on with, and is
 * voted on by the funnel of the context the match made. Leaving them at the first byte is the escape.
 */
final class MixedSteps implements StepRule {

    /** How many of the candidates a step ends with the next step tries again. */
    private static final int REPEATS = 2;
    /** A repeat weighs a quarter of the heaviest candidate of the funnel. */
    private static final int REPEAT_SHIFT = 2;
    private static final int SYMBOLS = 256;

    private final AnalogyDictionary dictionary;
    private final SegmentBuffer text;
    private final int segmentLength;
    private final int maxLength;
    private final DecisionPort port;
    private final ForecastedLiterals forecast = new ForecastedLiterals();
    private final MixedLiteralModel literals;
    private final Continuations next;
    private final MatchEndModel ends;
    private final ContinuationModel continuations;
    private final int[] repeats = new int[REPEATS];
    private int repeatCount;

    /**
     * @param text the bytes known so far: all of them for the encoder, those decoded for the decoder
     * @param maxLength the longest match, which is cut short there
     * @param funnelCapacity the most candidates a funnel holds
     */
    MixedSteps(AnalogyDictionary dictionary, SegmentBuffer text, int segmentLength, int maxLength,
               int funnelCapacity, DecisionPort port) {
        this.dictionary = dictionary;
        this.text = text;
        this.segmentLength = segmentLength;
        this.maxLength = maxLength;
        this.port = port;
        this.next = new Continuations(funnelCapacity + REPEATS);
        this.literals = new MixedLiteralModel(this.forecast, text, port,
                ContextBuckets.tableBits(segmentLength, 3, 22));
        this.ends = new MatchEndModel(ContextBuckets.tableBits(segmentLength, 0, 18));
        this.continuations = new ContinuationModel(ContextBuckets.tableBits(segmentLength, 1, 20));
    }

    @Override
    public int step(int idx) throws MalformedStreamException {
        Funnel funnel = this.dictionary.funnel(idx);
        this.forecast.reset();
        if (funnel.size() == 0) {
            this.repeatCount = 0;
            this.literals.code(idx);
            return 1;
        }
        this.next.start(funnel);
        for (int i = 0; i < this.repeatCount; i++) {
            this.next.addRepeat(this.repeats[i], Math.max(1, funnel.weight(0) >> REPEAT_SHIFT));
        }
        for (int depth = 0; ; depth++) {
            int at = idx + depth;
            if (depth == this.maxLength || at == this.segmentLength) {
                this.rememberRepeats(depth);
                return depth;
            }
            this.next.gather(this.text, depth, idx);
            int history = Byte.toUnsignedInt(this.text.byteAt(at - 2)) << 8
                    | Byte.toUnsignedInt(this.text.byteAt(at - 1));
            int truth = this.port.textByte(at);
            if (this.next.distinct() < SYMBOLS && this.ended(depth, history, truth)) {
                this.literalAfter(depth, at);
                return depth + 1;
            }
            int value = this.continuation(truth, depth, history);
            this.port.appended(value);
            this.next.keep(this.text, depth, value);
        }
    }

    /** Codes whether the text leaves every live candidate at this byte. */
    private boolean ended(int depth, int history, int truth) throws MalformedStreamException {
        int ended = this.decide(depth == 0 ? TripletFieldKind.DISTANCE : TripletFieldKind.LENGTH,
                this.next.rankOf(truth) < 0 ? 1 : 0, this.ends.probability(depth, this.next, history));
        this.ends.update(ended);
        return ended != 0;
    }

    /** Codes the literal where the text left the candidates, which cannot be a byte they went on with. */
    private void literalAfter(int depth, int at) throws MalformedStreamException {
        for (int rank = 0; rank < this.next.distinct(); rank++) {
            this.forecast.exclude(this.next.byteAt(rank));
        }
        if (depth > 0) {
            Funnel votes = this.dictionary.forecast(at);
            for (int i = 0; i < votes.size(); i++) {
                this.forecast.vote(Byte.toUnsignedInt(this.text.byteAt(votes.position(i))), votes.weight(i));
            }
            this.rememberRepeats(depth + 1);
        } else {
            this.repeatCount = 0;
        }
        this.literals.code(at);
    }

    /** Keeps the first live candidates, moved on by {@code shift} bytes, as the repeats of the next step. */
    private void rememberRepeats(int shift) {
        this.repeatCount = Math.min(REPEATS, this.next.liveCount());
        for (int i = 0; i < this.repeatCount; i++) {
            this.repeats[i] = this.next.livePosition(i) + shift;
        }
    }

    /** Codes which of the bytes the live candidates go on with the text takes, asking of each from the heaviest. */
    private int continuation(int truth, int depth, int history) throws MalformedStreamException {
        long remaining = this.next.total();
        int remainingCount = this.next.liveCount();
        int last = this.next.distinct() - 1;
        for (int rank = 0; rank < last; rank++) {
            int taken = this.decide(TripletFieldKind.DISTANCE, truth == this.next.byteAt(rank) ? 1 : 0,
                    this.continuations.probability(this.next, rank, remaining, remainingCount, depth, history));
            this.continuations.update(taken);
            if (taken != 0) {
                return this.next.byteAt(rank);
            }
            remaining -= this.next.weight(rank);
            remainingCount -= this.next.count(rank);
        }
        return this.next.byteAt(last);
    }

    private int decide(TripletFieldKind kind, int encoderBit, int probability) throws MalformedStreamException {
        return this.port.bit(kind, encoderBit, probability << (RangeEncoder.PROBABILITY_BITS - Logistic.BITS));
    }
}
