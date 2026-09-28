package cz.cvut.fit.acb.coding;

/** The model {@link AdaptiveFrequencyModel} is checked against: plain arrays and linear scans. */
final class NaiveFrequencyModel {

    private final int[] frequencies;
    private final int limit;

    NaiveFrequencyModel(int[] initial) {
        this.frequencies = initial.clone();
        this.limit = AdaptiveFrequencyModel.limitFor(initial.length);
        while (this.total() > this.limit) {
            this.halve();
        }
    }

    int total() {
        int total = 0;
        for (int frequency : this.frequencies) {
            total += frequency;
        }
        return total;
    }

    int frequency(int symbol) {
        return this.frequencies[symbol];
    }

    int cumulative(int symbol) {
        int sum = 0;
        for (int i = 0; i < symbol; i++) {
            sum += this.frequencies[i];
        }
        return sum;
    }

    int symbolAt(int target) {
        int symbol = 0;
        while (this.cumulative(symbol + 1) <= target) {
            symbol++;
        }
        return symbol;
    }

    void increment(int symbol) {
        if (this.total() + AdaptiveFrequencyModel.INCREMENT > this.limit) {
            this.halve();
        }
        this.frequencies[symbol] += AdaptiveFrequencyModel.INCREMENT;
    }

    private void halve() {
        for (int i = 0; i < this.frequencies.length; i++) {
            this.frequencies[i] = (this.frequencies[i] + 1) / 2;
        }
    }
}
