package cz.cvut.fit.acb.counts;

import java.util.Arrays;

/**
 * Prefix sums of a row of counts that change one at a time: a sum, an update and the search for the
 * entry that a target falls in each cost {@code log n}. The tree holds its own copy, so a caller
 * that keeps the counts too has to keep the two in step.
 */
public final class FenwickTree {

    private int[] tree;
    private int length;
    private int highestStep;

    /** @param capacity how many counts the tree holds before {@link #rebuild} has to grow it */
    public FenwickTree(int capacity) {
        this.tree = new int[capacity + 1];
    }

    /** Makes the tree hold {@code counts[0 .. length - 1]}. */
    public void rebuild(int[] counts, int length) {
        if (this.tree.length < length + 1) {
            this.tree = new int[Math.max(length + 1, this.tree.length * 2)];
        }
        Arrays.fill(this.tree, 0, length + 1, 0);
        for (int i = 1; i <= length; i++) {
            this.tree[i] += counts[i - 1];
            int parent = i + (i & -i);
            if (parent <= length) {
                this.tree[parent] += this.tree[i];
            }
        }
        this.length = length;
        this.highestStep = Integer.highestOneBit(length);
    }

    /** The sum of the first {@code count} counts. */
    public int sumBefore(int count) {
        int sum = 0;
        for (int i = count; i > 0; i -= i & -i) {
            sum += this.tree[i];
        }
        return sum;
    }

    public void add(int index, int delta) {
        for (int i = index + 1; i <= this.length; i += i & -i) {
            this.tree[i] += delta;
        }
    }

    /**
     * The index of the count whose range of the running sum contains {@code target}: the number of
     * counts that sum to {@code target} or less. Meant for non-negative counts.
     */
    public int indexAt(int target) {
        int position = 0;
        int remaining = target;
        for (int step = this.highestStep; step > 0; step >>= 1) {
            int next = position + step;
            if (next <= this.length && this.tree[next] <= remaining) {
                position = next;
                remaining -= this.tree[next];
            }
        }
        return position;
    }
}
