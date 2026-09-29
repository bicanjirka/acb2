package cz.cvut.fit.acb.triplets.coder;

import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.triplets.FieldBits;
import cz.cvut.fit.acb.triplets.FieldSink;
import cz.cvut.fit.acb.triplets.FieldSource;
import cz.cvut.fit.acb.triplets.Triplet;
import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletFieldKind;
import cz.cvut.fit.acb.triplets.TripletLayout;

/** {@code (dist, len, literal)}, every step; a literal alone has distance and length 0. */
public final class SimpleTripletLayout implements TripletLayout {

    private final int distanceBits;
    private final TripletFieldId distField;
    private final TripletFieldId lengField;
    private final TripletFieldId byteField;

    public SimpleTripletLayout(int distanceBits, int lengthBits) {
        this.distanceBits = distanceBits;
        this.distField = new TripletFieldId(0, distanceBits, TripletFieldKind.DISTANCE);
        this.lengField = new TripletFieldId(1, lengthBits, TripletFieldKind.LENGTH);
        this.byteField = new TripletFieldId(2, Byte.SIZE, TripletFieldKind.LITERAL);
    }

    @Override
    public void write(Triplet triplet, FieldSink sink) {
        switch (triplet) {
            case Triplet.Literal literal -> this.write(sink, 0, 0, literal.value());
            case Triplet.MatchWithLiteral match ->
                    this.write(sink, match.distance(), match.length(), match.literal());
            case Triplet.Match match -> throw new IllegalArgumentException("simple triplets always end in a literal");
        }
    }

    private void write(FieldSink sink, int distance, int length, byte literal) {
        sink.write(this.distField, FieldBits.lowBits(distance, this.distanceBits));
        sink.write(this.lengField, length);
        sink.write(this.byteField, literal & 0xFF);
    }

    @Override
    public Triplet read(FieldSource source) throws MalformedStreamException {
        int distance = FieldBits.signExtend(source.read(this.distField), this.distanceBits);
        int length = source.read(this.lengField);
        byte literal = (byte) source.read(this.byteField);
        return length == 0 ? Triplet.literal(literal) : Triplet.matchWithLiteral(distance, length, literal);
    }
}
