package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.counts.FenwickTree;

import java.util.Arrays;
import java.util.Objects;

/**
 * A two-level sorted array: chunks of sorted positions, split when full, with a Fenwick tree over
 * the chunk sizes that turns a chunk into a rank and back. A position costs thirteen bytes, a lookup two
 * binary searches, and a neighbour is one array step. Next to every position are the first eight bytes of
 * its context, which the searches compare first and so seldom read the text, and how many bytes of
 * context it has in common with the position before it, which a walk along the order uses to know how
 * far each entry agrees with a context without comparing bytes. The last two lookups are remembered,
 * so a position looked up and then inserted is found once. The storage and the search stay in one
 * class: a split into a store and an index measured several percent slower in decoding.
 */
public final class ChunkedContextIndex implements ContextIndex {

    private static final int DEFAULT_CHUNK_CAPACITY = 512;

    private final ContextOrder order;
    private final int chunkCapacity;
    private int[][] chunks = new int[4][];
    private long[][] prefixes = new long[4][];
    private byte[][] shared = new byte[4][];
    private int[] counts = new int[4];
    private long[] heads = new long[4];
    private final FenwickTree fenwick = new FenwickTree(4);
    private int chunkCount = 1;
    private int size;
    private int version;
    private final int[] rememberedPositions = {-1, -1};
    private final int[] rememberedChunks = new int[2];
    private final int[] rememberedOffsets = new int[2];
    private final int[] rememberedVersions = {-1, -1};
    private int overwritten;
    private int foundChunk;
    private int foundOffset;

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
        this.prefixes[0] = new long[chunkCapacity];
        this.shared[0] = new byte[chunkCapacity];
        this.fenwick.rebuild(this.counts, this.chunkCount);
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public int rank(int position) {
        this.locate(position);
        return this.fenwick.sumBefore(this.foundChunk) + this.foundOffset;
    }

    @Override
    public void insert(int position) {
        this.locate(position);
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
        int count = this.counts[chunk];
        int[] target = this.chunks[chunk];
        long[] targetPrefixes = this.prefixes[chunk];
        byte[] sharedBytes = this.shared[chunk];
        int predecessor = offset > 0 ? target[offset - 1]
                : chunk > 0 ? this.chunks[chunk - 1][this.counts[chunk - 1] - 1] : -1;
        long predecessorPrefix = offset > 0 ? targetPrefixes[offset - 1]
                : chunk > 0 ? this.prefixes[chunk - 1][this.counts[chunk - 1] - 1] : 0;
        int successor = offset < count ? target[offset]
                : chunk + 1 < this.chunkCount ? this.chunks[chunk + 1][0] : -1;
        long successorPrefix = offset < count ? targetPrefixes[offset]
                : chunk + 1 < this.chunkCount ? this.prefixes[chunk + 1][0] : 0;
        System.arraycopy(target, offset, target, offset + 1, count - offset);
        System.arraycopy(targetPrefixes, offset, targetPrefixes, offset + 1, count - offset);
        System.arraycopy(sharedBytes, offset, sharedBytes, offset + 1, count - offset);
        target[offset] = position;
        long prefix = this.order.prefix(position);
        targetPrefixes[offset] = prefix;
        if (offset == 0) {
            this.heads[chunk] = prefix;
        }
        sharedBytes[offset] = predecessor < 0 ? 0
                : (byte) this.order.sharedBytes(predecessor, predecessorPrefix, position, prefix);
        if (successor >= 0) {
            byte toSuccessor = (byte) this.order.sharedBytes(position, prefix, successor, successorPrefix);
            if (offset < count) {
                sharedBytes[offset + 1] = toSuccessor;
            } else {
                this.shared[chunk + 1][0] = toSuccessor;
            }
        }
        this.counts[chunk]++;
        this.fenwick.add(chunk, 1);
        this.size++;
        this.version++;
    }

    @Override
    public ContextCursor cursorAt(int rank) {
        Objects.checkIndex(rank, this.size);
        int chunk = this.fenwick.indexAt(rank);
        return new Cursor(chunk, rank - this.fenwick.sumBefore(chunk), rank);
    }

    @Override
    public void around(int position, int reach, Surroundings into) {
        into.clear();
        if (this.size == 0) {
            return;
        }
        this.locate(position);
        int chunk = this.foundChunk;
        int offset = this.foundOffset;
        int left = reach;
        while (left > 0) {
            if (offset == this.counts[chunk]) {
                if (++chunk == this.chunkCount) {
                    break;
                }
                offset = 0;
            }
            int run = Math.min(left, this.counts[chunk] - offset);
            into.addAbove(this.chunks[chunk], this.prefixes[chunk], this.shared[chunk], offset, run);
            left -= run;
            offset += run;
        }
        chunk = this.foundChunk;
        offset = this.foundOffset;
        left = reach;
        while (left > 0) {
            if (offset == 0) {
                if (--chunk < 0) {
                    break;
                }
                offset = this.counts[chunk];
            }
            int run = Math.min(left, offset);
            into.addBelow(this.chunks[chunk], this.prefixes[chunk], this.shared[chunk], offset - run, run);
            left -= run;
            offset -= run;
        }
    }

    /** Finds where {@code position} belongs, in {@link #foundChunk} and {@link #foundOffset}. */
    private void locate(int position) {
        for (int slot = 0; slot < this.rememberedPositions.length; slot++) {
            if (this.rememberedPositions[slot] == position && this.rememberedVersions[slot] == this.version) {
                this.foundChunk = this.rememberedChunks[slot];
                this.foundOffset = this.rememberedOffsets[slot];
                return;
            }
        }
        long prefix = this.order.prefix(position);
        int chunk = 0;
        if (this.size > 0) {
            int low = 1;
            int high = this.chunkCount - 1;
            while (low <= high) {
                int middle = (low + high) >>> 1;
                if (this.sortsBefore(this.heads[middle], this.chunks[middle], 0, prefix, position)) {
                    chunk = middle;
                    low = middle + 1;
                } else {
                    high = middle - 1;
                }
            }
        }
        int[] entries = this.chunks[chunk];
        long[] entryPrefixes = this.prefixes[chunk];
        int low = 0;
        int high = this.counts[chunk];
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (this.sortsBefore(entryPrefixes[middle], entries, middle, prefix, position)) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        this.foundChunk = chunk;
        this.foundOffset = low;
        int slot = this.overwritten;
        this.overwritten = 1 - slot;
        this.rememberedPositions[slot] = position;
        this.rememberedChunks[slot] = chunk;
        this.rememberedOffsets[slot] = low;
        this.rememberedVersions[slot] = this.version;
    }

    /** Whether the entry sorts before {@code position}; the bytes are read only if the prefixes are equal. */
    private boolean sortsBefore(long entryPrefix, int[] entries, int at, long prefix, int position) {
        if (entryPrefix != prefix) {
            return Long.compareUnsigned(entryPrefix, prefix) < 0;
        }
        return this.order.compare(entries[at], position) < 0;
    }

    private void splitChunk(int chunk, int kept) {
        if (this.chunkCount == this.chunks.length) {
            this.chunks = Arrays.copyOf(this.chunks, this.chunkCount * 2);
            this.prefixes = Arrays.copyOf(this.prefixes, this.chunkCount * 2);
            this.shared = Arrays.copyOf(this.shared, this.chunkCount * 2);
            this.counts = Arrays.copyOf(this.counts, this.chunkCount * 2);
            this.heads = Arrays.copyOf(this.heads, this.chunkCount * 2);
        }
        int moved = this.counts[chunk] - kept;
        System.arraycopy(this.chunks, chunk + 1, this.chunks, chunk + 2, this.chunkCount - chunk - 1);
        System.arraycopy(this.prefixes, chunk + 1, this.prefixes, chunk + 2, this.chunkCount - chunk - 1);
        System.arraycopy(this.shared, chunk + 1, this.shared, chunk + 2, this.chunkCount - chunk - 1);
        System.arraycopy(this.counts, chunk + 1, this.counts, chunk + 2, this.chunkCount - chunk - 1);
        System.arraycopy(this.heads, chunk + 1, this.heads, chunk + 2, this.chunkCount - chunk - 1);
        int[] upper = new int[this.chunkCapacity];
        long[] upperPrefixes = new long[this.chunkCapacity];
        byte[] upperShared = new byte[this.chunkCapacity];
        System.arraycopy(this.chunks[chunk], kept, upper, 0, moved);
        System.arraycopy(this.prefixes[chunk], kept, upperPrefixes, 0, moved);
        System.arraycopy(this.shared[chunk], kept, upperShared, 0, moved);
        this.chunks[chunk + 1] = upper;
        this.prefixes[chunk + 1] = upperPrefixes;
        this.shared[chunk + 1] = upperShared;
        this.heads[chunk + 1] = upperPrefixes[0];
        this.counts[chunk + 1] = moved;
        this.counts[chunk] = kept;
        this.chunkCount++;
        this.fenwick.rebuild(this.counts, this.chunkCount);
    }

    private final class Cursor implements ContextCursor {

        private final int madeAt = ChunkedContextIndex.this.version;
        private int chunk;
        private int offset;
        /** -1 until asked for: a walk seldom needs the rank, and finding it takes a sum over the chunks. */
        private int rank;

        private Cursor(int chunk, int offset, int rank) {
            this.chunk = chunk;
            this.offset = offset;
            this.rank = rank;
        }

        @Override
        public int rank() {
            this.requireUnchanged();
            if (this.rank < 0) {
                this.rank = ChunkedContextIndex.this.fenwick.sumBefore(this.chunk) + this.offset;
            }
            return this.rank;
        }

        @Override
        public int position() {
            this.requireUnchanged();
            return ChunkedContextIndex.this.chunks[this.chunk][this.offset];
        }

        @Override
        public int sharedWithPrevious() {
            this.requireUnchanged();
            return ChunkedContextIndex.this.shared[this.chunk][this.offset] & 0xFF;
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
            if (this.rank >= 0) {
                this.rank++;
            }
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
            if (this.rank >= 0) {
                this.rank--;
            }
            return true;
        }

        private void requireUnchanged() {
            if (this.madeAt != ChunkedContextIndex.this.version) {
                throw new IllegalStateException("The index changed after this cursor was made");
            }
        }
    }
}
