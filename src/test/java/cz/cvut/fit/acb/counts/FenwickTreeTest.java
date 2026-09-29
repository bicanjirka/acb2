package cz.cvut.fit.acb.counts;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FenwickTreeTest {

    @Test
    void sumsBeforeAnIndexAreTheRunningTotalsOfTheCounts() {
        FenwickTree tree = new FenwickTree(4);

        tree.rebuild(new int[] {3, 0, 5, 2}, 4);

        assertThat(new int[] {tree.sumBefore(0), tree.sumBefore(1), tree.sumBefore(2), tree.sumBefore(3),
                tree.sumBefore(4)}).containsExactly(0, 3, 3, 8, 10);
    }

    @Test
    void anAddedDeltaShowsInEverySumAfterItsIndexOnly() {
        FenwickTree tree = new FenwickTree(4);
        tree.rebuild(new int[] {1, 1, 1, 1}, 4);

        tree.add(2, 5);

        assertThat(new int[] {tree.sumBefore(2), tree.sumBefore(3), tree.sumBefore(4)}).containsExactly(2, 8, 9);
    }

    @Test
    void aTargetFindsTheCountWhoseRangeContainsIt() {
        FenwickTree tree = new FenwickTree(4);
        tree.rebuild(new int[] {3, 0, 5, 2}, 4);

        assertThat(new int[] {tree.indexAt(0), tree.indexAt(2), tree.indexAt(3), tree.indexAt(7), tree.indexAt(8),
                tree.indexAt(9)}).containsExactly(0, 0, 2, 2, 3, 3);
    }

    @Test
    void rebuildingWithMoreCountsThanTheCapacityGrowsTheTree() {
        FenwickTree tree = new FenwickTree(1);

        tree.rebuild(new int[] {1, 2, 3, 4, 5}, 5);

        assertThat(tree.sumBefore(5)).isEqualTo(15);
        assertThat(tree.indexAt(6)).isEqualTo(3);
    }

    @Test
    void rebuildingWithFewerCountsForgetsTheOnesLeftOut() {
        FenwickTree tree = new FenwickTree(4);
        tree.rebuild(new int[] {1, 2, 3, 4}, 4);

        tree.rebuild(new int[] {7, 7, 9, 9}, 2);

        assertThat(tree.sumBefore(2)).isEqualTo(14);
        assertThat(tree.indexAt(100)).isEqualTo(2);
    }
}
