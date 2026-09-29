package cz.cvut.fit.acb.triplets.coder;

import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.triplets.FieldSink;
import cz.cvut.fit.acb.triplets.FieldSource;
import cz.cvut.fit.acb.triplets.Triplet;
import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletFieldKind;
import cz.cvut.fit.acb.triplets.TripletLayout;
import cz.cvut.fit.acb.utils.BitUtils;

/**
 * A flag, then {@code (0, literal)} for a literal alone and {@code (1, dist, len)} for a match with
 * no literal after it, as Salomon has it. The {@link #withLiteral} form carries the literal on
 * matches too, {@code (1, dist, len, literal)}, which ExCom calls Salomon2.
 */
public final class SalomonTripletLayout implements TripletLayout {

    private static final int FLAG_BITS = 1;

    private final int distanceBits;
    private final boolean literalAfterMatch;
    private final TripletFieldId flagField;
    private final TripletFieldId distField;
    private final TripletFieldId lengField;
    private final TripletFieldId byteField;

    private SalomonTripletLayout(int distanceBits, int lengthBits, boolean literalAfterMatch) {
        this.distanceBits = distanceBits;
        this.literalAfterMatch = literalAfterMatch;
        this.flagField = new TripletFieldId(0, FLAG_BITS, TripletFieldKind.FLAG);
        this.distField = new TripletFieldId(1, distanceBits, TripletFieldKind.DISTANCE);
        this.lengField = new TripletFieldId(2, lengthBits, TripletFieldKind.LENGTH);
        this.byteField = new TripletFieldId(3, Byte.SIZE, TripletFieldKind.LITERAL);
    }

    /** Salomon's own form: a match carries no literal. */
    public static SalomonTripletLayout bare(int distanceBits, int lengthBits) {
        return new SalomonTripletLayout(distanceBits, lengthBits, false);
    }

    /** The flagged form with the literal kept on matches. */
    public static SalomonTripletLayout withLiteral(int distanceBits, int lengthBits) {
        return new SalomonTripletLayout(distanceBits, lengthBits, true);
    }

    @Override
    public void write(Triplet triplet, FieldSink sink) {
        switch (triplet) {
            case Triplet.Literal literal -> {
                sink.write(this.flagField, 0);
                sink.write(this.byteField, literal.value() & 0xFF);
            }
            case Triplet.Match match -> {
                this.requireForm(false);
                this.writeMatch(sink, match.distance(), match.length());
            }
            case Triplet.MatchWithLiteral match -> {
                this.requireForm(true);
                this.writeMatch(sink, match.distance(), match.length());
                sink.write(this.byteField, match.literal() & 0xFF);
            }
        }
    }

    private void writeMatch(FieldSink sink, int distance, int length) {
        sink.write(this.flagField, 1);
        sink.write(this.distField, BitUtils.lowBits(distance, this.distanceBits));
        sink.write(this.lengField, length);
    }

    private void requireForm(boolean withLiteral) {
        if (withLiteral != this.literalAfterMatch) {
            throw new IllegalArgumentException("this layout's matches "
                    + (this.literalAfterMatch ? "end in a literal" : "carry no literal"));
        }
    }

    @Override
    public Triplet read(FieldSource source) throws MalformedStreamException {
        if (source.read(this.flagField) == 0) {
            return Triplet.literal((byte) source.read(this.byteField));
        }
        int distance = BitUtils.signExtend(source.read(this.distField), this.distanceBits);
        int length = source.read(this.lengField);
        if (length == 0) {
            throw new MalformedStreamException("A flagged triplet must match at least one byte");
        }
        return this.literalAfterMatch
                ? Triplet.matchWithLiteral(distance, length, (byte) source.read(this.byteField))
                : Triplet.match(distance, length);
    }
}
