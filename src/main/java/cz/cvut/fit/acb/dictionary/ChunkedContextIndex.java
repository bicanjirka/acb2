package cz.cvut.fit.acb.dictionary;

import java.util.Arrays;
import java.util.Objects;

/**
 * A two-level sorted array: chunks of sorted positions, split when full, with a Fenwick tree over
 * the chunk sizes that turns a chunk into a rank and back. A position costs four bytes, a lookup
 * two binary searches, and a neighbour is one array step.
 */
public final class ChunkedContextIndex implements ContextIndex {

    private static final int DEFAULT_CHUNK_CAPACITY = 512;

    private final ContextOrder order;
    private final int chunkCapacity;
    private int[][] chunks = new int[4][];
    private int[] counts = new int[4];
    private int[] fenwick = new int[5];
    private int chunkCount = 1;
    private int size;
    private int version;
    private int foundPosition = -1;
    private int foundChunk;
    private int foundOffset;
    private int foundVersion = -1;

    public ChunkedContextIndex(ContextOrder order) {
        this(order, DEFAULT_CHUNK_CAPACITY);
    }

    ChunkedContextIndex(ContextOrder order, int chunkCapacity) {
        if (chunkCapacity < 2) {
            throw new IllegalArgumentException("a chunk holds at least two positions: " + chunkCapacity);
        }
        this.order = order;
        this.chunkCapacity = chunkCapacity;
        this.chunks[0] = new int[chunkCapacity];
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public int rank(int position) {
        this.locate(position);
        return this.entriesBefore(this.foundChunk) + this.foundOffset;
    }

    @Override
    public void insert(int position) {
        if (position != this.foundPosition || this.version != this.foundVersion) {
            this.locate(position);
        }
        int chunk = this.foundChunk;
        int offset = this.foundOffset;
        if (this.counts[chunk] == this.chunkCapacity) {
            int kept = this.chunkCapacity / 2;
            this.splitChunk(chunk, kept);
            if (offset > kept) {
                chunk++;
                offset -= kept;
            }
        }
        int[] target = this.chunks[chunk];
        System.arraycopy(target, offset, target, offset + 1, this.counts[chunk] - offset);
        target[offset] = position;
        this.counts[chunk]++;
        this.addToFenwick(chunk);
        this.size++;
        this.version++;
    }

    @Override
    public ContextCursor cursorAt(int rank) {
        Objects.checkIndex(rank, this.size);
        int remaining = rank;
        int chunk = 0;
        for (int step = Integer.highestOneBit(this.chunkCount); step > 0; step >>= 1) {
            int next = chunk + step;
            if (next <= this.chunkCount && this.fenwick[next] <= remaining) {
                chunk = next;
                remaining -= this.fenwick[next];
            }
        }
        return new Cursor(chunk, remaining, rank);
    }

    /** Finds where {@code position} belongs and remembers it for an {@link #insert} right after. */
    private void locate(int position) {
        int chunk = 0;
        if (this.size > 0) {
            int low = 1;
            int high = this.chunkCount - 1;
            while (low <= high) {
                int middle = (low + high) >>> 1;
                if (this.order.compare(this.chunks[middle][0], position) < 0) {
                    chunk = middle;
                    low = middle + 1;
                } else {
                    high = middle - 1;
                }
            }
        }
        int[] entries = this.chunks[chunk];
        int low = 0;
        int high = this.counts[chunk];
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (this.order.compare(entries[middle], position) < 0) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        this.foundPosition = position;
        this.foundChunk = chunk;
        this.foundOffset = low;
        this.foundVersion = this.version;
    }

    private void splitChunk(int chunk, int kept) {
        if (this.chunkCount == this.chunks.length) {
            this.chunks = Arrays.copyOf(this.chunks, this.chunkCount * 2);
            this.counts = Arrays.copyOf(this.counts, this.chunkCount * 2);
        }
        int moved = this.counts[chunk] - kept;
        System.arraycopy(this.chunks, chunk + 1, this.chunks, chunk + 2, this.chunkCount - chunk - 1);
        System.arraycopy(this.counts, chunk + 1, this.counts, chunk + 2, this.chunkCount - chunk - 1);
        int[] upper = new int[this.chunkCapacity];
        System.arraycopy(this.chunks[chunk], kept, upper, 0, moved);
        this.chunks[chunk + 1] = upper;
        this.counts[chunk + 1] = moved;
        this.counts[chunk] = kept;
        this.chunkCount++;
        this.rebuildFenwick();
    }

    private int entriesBefore(int chunk) {
        int sum = 0;
        for (int i = chunk; i > 0; i -= i & -i) {
            sum += this.fenwick[i];
        }
        return sum;
    }

    private void addToFenwick(int chunk) {
        for (int i = chunk + 1; i <= this.chunkCount; i += i & -i) {
            this.fenwick[i]++;
        }
    }

    private void rebuildFenwick() {
        if (this.fenwick.length < this.chunkCount + 1) {
            this.fenwick = new int[this.chunks.length + 1];
        }
        for (int i = 1; i <= this.chunkCount; i++) {
            this.fenwick[i] = this.counts[i - 1];
        }
        for (int i = 1; i <= this.chunkCount; i++) {
            int parent = i + (i & -i);
            if (parent <= this.chunkCount) {
                this.fenwick[parent] += this.fenwick[i];
            }
        }
    }

    private final class Cursor implements ContextCursor {

        private final int madeAt = ChunkedContextIndex.this.version;
        private int chunk;
        private int offset;
        private int rank;

        private Cursor(int chunk, int offset, int rank) {
            this.chunk = chunk;
            this.offset = offset;
            this.rank = rank;
        }

        @Override
        public int rank() {
            return this.rank;
        }

        @Override
        public int position() {
            this.requireUnchanged();
            return ChunkedContextIndex.this.chunks[this.chunk][this.offset];
        }

        @Override
        public boolean moveUp() {
            this.requireUnchanged();
            if (this.offset + 1 < ChunkedContextIndex.this.counts[this.chunk]) {
                this.offset++;
            } else if (this.chunk + 1 < ChunkedContextIndex.this.chunkCount) {
                this.chunk++;
                this.offset = 0;
            } else {
                return false;
            }
            this.rank++;
            return true;
        }

        @Override
        public boolean moveDown() {
            this.requireUnchanged();
            if (this.offset > 0) {
                this.offset--;
            } else if (this.chunk > 0) {
                this.chunk--;
                this.offset = ChunkedContextIndex.this.counts[this.chunk] - 1;
            } else {
                return false;
            }
            this.rank--;
            return true;
        }

        private void requireUnchanged() {
            if (this.madeAt != ChunkedContextIndex.this.version) {
                throw new IllegalStateException("The index changed after this cursor was made");
            }
        }
    }
}
