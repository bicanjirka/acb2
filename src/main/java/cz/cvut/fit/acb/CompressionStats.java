package cz.cvut.fit.acb;

import cz.cvut.fit.acb.coding.FieldCost;
import cz.cvut.fit.acb.triplets.TripletFieldKind;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * What one compression run did: how much it read, how many triplets it made, and what each kind
 * of triplet field cost in the payload, in the order of {@link TripletFieldKind}. Stats of separate runs add up; {@link #none()} is the
 * identity.
 */
public record CompressionStats(long inputBytes, long segments, long triplets, List<FieldCost> fields) {

    public CompressionStats {
        fields = fields.stream().sorted(Comparator.comparing(FieldCost::kind)).toList();
    }

    public static CompressionStats none() {
        return new CompressionStats(0, 0, 0, List.of());
    }

    public CompressionStats plus(CompressionStats other) {
        List<FieldCost> merged = Arrays.stream(TripletFieldKind.values())
                .map(kind -> this.cost(kind).plus(other.cost(kind)))
                .filter(cost -> cost.symbols() > 0)
                .toList();
        return new CompressionStats(this.inputBytes + other.inputBytes, this.segments + other.segments,
                this.triplets + other.triplets, merged);
    }

    /** The cost of one kind of field; a coder without that field costs {@link FieldCost#none}. */
    public FieldCost cost(TripletFieldKind kind) {
        return this.fields.stream().filter(cost -> cost.kind() == kind).findFirst().orElse(FieldCost.none(kind));
    }

    public long fieldBits() {
        return this.fields.stream().mapToLong(FieldCost::bits).sum();
    }
}
