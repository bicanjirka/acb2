package cz.cvut.fit.acb.coding;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * The frequencies a length field's model starts from, one per length in order; lengths past the
 * list start at 1. {@link #flat()} is the empty list, where every length starts at 1.
 */
public record LengthFrequencies(List<Integer> values) {

    private static final LengthFrequencies FLAT = new LengthFrequencies(List.of());

    /** @throws IllegalArgumentException if a frequency is not positive */
    public LengthFrequencies {
        values = List.copyOf(values);
        if (values.stream().anyMatch(frequency -> frequency <= 0)) {
            throw new IllegalArgumentException("length frequencies must be greater than zero: " + values);
        }
    }

    public static LengthFrequencies flat() {
        return FLAT;
    }

    /** @throws IllegalArgumentException if a frequency is not positive */
    public static LengthFrequencies of(int... frequencies) {
        return new LengthFrequencies(Arrays.stream(frequencies).boxed().toList());
    }

    /** The table of a model of {@code symbols} symbols: these frequencies, cut or padded with 1s. */
    public int[] startingTable(int symbols) {
        return IntStream.range(0, symbols).map(length -> length < this.values.size() ? this.values.get(length) : 1)
                .toArray();
    }

    /** Only the frequencies of the first {@code symbols} lengths, which are all the coder uses. */
    public LengthFrequencies limitedTo(int symbols) {
        return this.values.size() <= symbols ? this : new LengthFrequencies(this.values.subList(0, symbols));
    }

    public int[] toArray() {
        return this.values.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Frequencies past the alphabet are ignored by the coder and every symbol they leave out starts
     * at 1; this checks that the model's starting total, counted that way, is small enough.
     *
     * @throws IllegalArgumentException if the starting total is more than the model is halved from
     */
    public void requireFits(int symbols) {
        long total = IntStream.of(this.startingTable(symbols)).asLongStream().sum();
        long limit = AdaptiveFrequencyModel.limitFor(symbols);
        if (total > limit) {
            throw new IllegalArgumentException("length frequencies total " + total + ", more than " + limit
                    + ", the total their model is halved from");
        }
    }
}
