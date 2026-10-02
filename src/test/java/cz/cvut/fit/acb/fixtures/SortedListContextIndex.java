package cz.cvut.fit.acb.fixtures;

import cz.cvut.fit.acb.dictionary.ContextCursor;
import cz.cvut.fit.acb.dictionary.ContextIndex;
import cz.cvut.fit.acb.dictionary.ContextOrder;
import cz.cvut.fit.acb.dictionary.Surroundings;

import java.util.ArrayList;
import java.util.List;

/** The obviously correct {@link ContextIndex}: a sorted list searched linearly, to check the fast ones against. */
public final class SortedListContextIndex implements ContextIndex {

    private final ContextOrder order;
    private final List<Integer> entries = new ArrayList<>();

    public SortedListContextIndex(ContextOrder order) {
        this.order = order;
    }

    @Override
    public int size() {
        return this.entries.size();
    }

    @Override
    public int rank(int position) {
        int before = 0;
        for (int entry : this.entries) {
            if (this.order.compare(entry, position) < 0) {
                before++;
            }
        }
        return before;
    }

    @Override
    public void insert(int position) {
        this.entries.add(this.rank(position), position);
    }

    @Override
    public ContextCursor cursorAt(int rank) {
        return new ListCursor(rank);
    }

    @Override
    public void around(int position, int reach, Surroundings into) {
        into.clear();
        int rank = this.rank(position);
        if (rank < this.entries.size()) {
            into.agreeAbove(this.order.sharedBits(position, this.entries.get(rank)));
        }
        if (rank > 0) {
            into.agreeBelow(this.order.sharedBits(position, this.entries.get(rank - 1)));
        }
        this.copy(rank, Math.min(this.entries.size(), rank + reach), into, true);
        this.copy(Math.max(0, rank - reach), rank, into, false);
    }

    /** The entries of ranks {@code from .. to - 1} as a run of the index, the way a real index hands them over. */
    private void copy(int from, int to, Surroundings into, boolean above) {
        int count = to - from;
        int[] positions = new int[count];
        int[] shared = new int[count];
        for (int i = 0; i < count; i++) {
            positions[i] = this.entries.get(from + i);
            shared[i] = Surroundings.shared(this.cursorAt(from + i).sharedWithPrevious(),
                    this.order.followingByte(positions[i]));
        }
        if (above) {
            into.addAbove(positions, shared, 0, count);
        } else {
            into.addBelow(positions, shared, 0, count);
        }
    }

    private final class ListCursor implements ContextCursor {

        private int rank;

        private ListCursor(int rank) {
            this.rank = rank;
        }

        @Override
        public int rank() {
            return this.rank;
        }

        @Override
        public int position() {
            return SortedListContextIndex.this.entries.get(this.rank);
        }

        @Override
        public int sharedWithPrevious() {
            if (this.rank == 0) {
                return 0;
            }
            return SortedListContextIndex.this.order.sharedBits(
                    SortedListContextIndex.this.entries.get(this.rank - 1), this.position());
        }

        @Override
        public boolean moveUp() {
            if (this.rank + 1 >= SortedListContextIndex.this.entries.size()) {
                return false;
            }
            this.rank++;
            return true;
        }

        @Override
        public boolean moveDown() {
            if (this.rank == 0) {
                return false;
            }
            this.rank--;
            return true;
        }
    }
}
