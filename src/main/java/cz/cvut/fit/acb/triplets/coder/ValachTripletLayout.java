package cz.cvut.fit.acb.triplets.coder;

import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.triplets.FieldSink;
import cz.cvut.fit.acb.triplets.FieldSource;
import cz.cvut.fit.acb.triplets.Triplet;
import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletFieldKind;
import cz.cvut.fit.acb.triplets.TripletLayout;
import cz.cvut.fit.acb.utils.BitUtils;

/** {@code (len, literal)} for a literal alone, {@code (len, dist, literal)} for a match. */
public final class ValachTripletLayout implements TripletLayout {

    private final int distanceBits;
    private final TripletFieldId lengField;
    private final TripletFieldId distField;
    private final TripletFieldId byteField;

    public ValachTripletLayout(int distanceBits, int lengthBits) {
        this.distanceBits = distanceBits;
        this.lengField = new TripletFieldId(0, lengthBits, TripletFieldKind.LENGTH);
        this.distField = new TripletFieldId(1, distanceBits, TripletFieldKind.DISTANCE);
        this.byteField = new TripletFieldId(2, Byte.SIZE, TripletFieldKind.LITERAL);
    }

    @Override
    public void write(Triplet triplet, FieldSink sink) {
        switch (triplet) {
            case Triplet.Literal literal -> {
                sink.write(this.lengField, 0);
                sink.write(this.byteField, literal.value() & 0xFF);
            }
            case Triplet.MatchWithLiteral match -> {
                sink.write(this.lengField, match.length());
                sink.write(this.distField, BitUtils.lowBits(match.distance(), this.distanceBits));
                sink.write(this.byteField, match.literal() & 0xFF);
            }
            case Triplet.Match match -> throw new IllegalArgumentException("valach triplets always end in a literal");
        }
    }

    @Override
    public Triplet read(FieldSource source) throws MalformedStreamException {
        int length = source.read(this.lengField);
        if (length == 0) {
            return Triplet.literal((byte) source.read(this.byteField));
        }
        int distance = BitUtils.signExtend(source.read(this.distField), this.distanceBits);
        return Triplet.matchWithLiteral(distance, length, (byte) source.read(this.byteField));
    }
}
