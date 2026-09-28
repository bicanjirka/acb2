package cz.cvut.fit.acb.fixtures;

import cz.cvut.fit.acb.dictionary.ContextCursor;
import cz.cvut.fit.acb.dictionary.ContextIndex;
import cz.cvut.fit.acb.dictionary.ContextOrder;

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
