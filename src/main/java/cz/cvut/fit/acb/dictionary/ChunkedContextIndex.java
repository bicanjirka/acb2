package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.counts.FenwickTree;

import java.util.Arrays;
import java.util.Objects;

/**
 * A two-level sorted array: chunks of sorted positions, split when full, with a Fenwick tree over
 * the chunk sizes that turns a chunk into a rank and back, built the first time a rank is asked for. A
 * position costs twelve bytes, a lookup two binary searches, and a neighbour is one array step. Next to
 * every position are the first eight bytes of its context, which the searches compare first and so seldom
 * read the text. An index {@link #forWalks for walks} keeps four bytes more, in one {@code int} how many bits
 * of context the position has in common with the one before it and the byte that follows its context, from
 * which a walk along the order knows how far each entry agrees with a context and what it goes on with,
 * without reading the text; an index {@link #forRanks for ranks} keeps none, since keeping them made the coders
 * of triplets, which never walk, a tenth slower to compress and a sixth slower to decode. The last two places
 * looked up to walk around or to rank are remembered and kept right through the inserts after them, so that a
 * place looked up and inserted at a few inserts later is found once. The storage and the search stay in one
 * class: a split into a store and an index measured several percent slower in decoding.
 */
public final class ChunkedContextIndex implements ContextIndex {

    private static final int DEFAULT_CHUNK_CAPACITY = 512;

    private final ContextOrder order;
    private final int chunkCapacity;
    private final boolean walks;
    private int[][] chunks = new int[4][];
    private long[][] prefixes = new long[4][];
    private int[][] shared = new int[4][];
    private int[] counts = new int[4];
    private long[] heads = new long[4];
    private final FenwickTree fenwick = new FenwickTree(4);
    private int chunkCount = 1;
    /** Whether the tree is kept: only once a rank is asked for, which the associative coder never does. */
    private boolean ranked;
    private int size;
    private int version;
    private final int[] rememberedPositions = {-1, -1};
    private final int[] rememberedChunks = new int[2];
    private final int[] rememberedOffsets = new int[2];
    private int overwritten;
    private int foundChunk;
    private int foundOffset;

    /** @param walks whether it keeps what {@link #around} hands out */
    ChunkedContextIndex(ContextOrder order, int chunkCapacity, boolean walks) {
        if (chunkCapacity < 2) {
            throw new IllegalArgumentException("a chunk holds at least two positions: " + chunkCapacity);
        }
        this.order = order;
        this.chunkCapacity = chunkCapacity;
        this.walks = walks;
        this.chunks[0] = new int[chunkCapacity];
        this.prefixes[0] = new long[chunkCapacity];
        if (walks) {
            this.shared[0] = new int[chunkCapacity];
        }
    }

    /** An index that ranks and walks: {@link #around} and {@link ContextCursor#sharedWithPrevious} can be asked. */
    public static ChunkedContextIndex forWalks(ContextOrder order) {
        return new ChunkedContextIndex(order, DEFAULT_CHUNK_CAPACITY, true);
    }

    /** An index that only ranks: {@link #around} and {@link ContextCursor#sharedWithPrevious} are refused. */
    public static ChunkedContextIndex forRanks(ContextOrder order) {
        return new ChunkedContextIndex(order, DEFAULT_CHUNK_CAPACITY, false);
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public int rank(int position) {
        this.locate(position, true);
        return this.ranks().sumBefore(this.foundChunk) + this.foundOffset;
    }

    @Override
    public void insert(int position) {
        this.locate(position, false);
        int chunk = this.foundChunk;
        int offset = this.foundOffset;
        if (this.counts[chunk] == this.chunkCapacity) {
            int kept = this.chunkCapacity / 2;
            this.splitChunk(chunk, kept);
            this.rememberSplit(chunk, kept);
            if (offset > kept) {
                chunk++;
                offset -= kept;
            }
        }
        int count = this.counts[chunk];
        int[] target = this.chunks[chunk];
        long[] targetPrefixes = this.prefixes[chunk];
        long prefix = this.order.prefix(position);
        if (this.walks) {
            this.share(position, prefix, chunk, offset, count);
        }
        System.arraycopy(target, offset, target, offset + 1, count - offset);
        System.arraycopy(targetPrefixes, offset, targetPrefixes, offset + 1, count - offset);
        target[offset] = position;
        targetPrefixes[offset] = prefix;
        if (offset == 0) {
            this.heads[chunk] = prefix;
        }
        this.counts[chunk]++;
        if (this.ranked) {
            this.fenwick.add(chunk, 1);
        }
        this.size++;
        this.version++;
        this.rememberInsert(position, chunk, offset);
    }

    /**
     * Makes room for {@code position} at {@code offset} of {@code chunk}, which holds {@code count} entries,
     * among what is kept for walks, and keeps there what it shares with the entries on either side of it; the
     * entries themselves are not moved yet.
     */
    private void share(int position, long prefix, int chunk, int offset, int count) {
        int fromPredecessor = 0;
        if (offset > 0 || chunk > 0) {
            int before = offset > 0 ? chunk : chunk - 1;
            int at = offset > 0 ? offset - 1 : this.counts[before] - 1;
            fromPredecessor = this.order.sharedBits(this.chunks[before][at], this.prefixes[before][at], position,
                    prefix);
        }
        int after = offset < count ? chunk : chunk + 1;
        int successor = offset < count ? offset : 0;
        boolean followed = after < this.chunkCount;
        int toSuccessor = followed ? this.order.sharedBits(position, prefix, this.chunks[after][successor],
                this.prefixes[after][successor]) : 0;
        int[] sharedBits = this.shared[chunk];
        System.arraycopy(sharedBits, offset, sharedBits, offset + 1, count - offset);
        sharedBits[offset] = Surroundings.shared(fromPredecessor, this.order.followingByte(position));
        if (followed) {
            int[] successorShared = this.shared[after];
            int at = after == chunk ? offset + 1 : 0;
            successorShared[at] = Surroundings.shared(toSuccessor, Surroundings.followingByte(successorShared[at]));
        }
    }

    @Override
    public ContextCursor cursorAt(int rank) {
        Objects.checkIndex(rank, this.size);
        FenwickTree ranks = this.ranks();
        int chunk = ranks.indexAt(rank);
        return new Cursor(chunk, rank - ranks.sumBefore(chunk), rank);
    }

    @Override
    public void around(int position, int reach, Surroundings into) {
        this.requireWalks();
        into.clear();
        if (this.size == 0) {
            return;
        }
        this.locate(position, true);
        long prefix = this.order.prefix(position);
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
            if (into.aboveCount() == 0) {
                into.agreeAbove(this.order.sharedBits(position, prefix, this.chunks[chunk][offset],
                        this.prefixes[chunk][offset]));
            }
            into.addAbove(this.chunks[chunk], this.shared[chunk], offset, run);
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
            if (into.belowCount() == 0) {
                into.agreeBelow(this.order.sharedBits(position, prefix, this.chunks[chunk][offset - 1],
                        this.prefixes[chunk][offset - 1]));
            }
            into.addBelow(this.chunks[chunk], this.shared[chunk], offset - run, run);
            left -= run;
            offset -= run;
        }
    }

    /**
     * Finds where {@code position} belongs, in {@link #foundChunk} and {@link #foundOffset}, and remembers it if
     * asked to: a place looked up to walk around is often inserted at a few inserts later.
     */
    private void locate(int position, boolean remember) {
        for (int slot = 0; slot < this.rememberedPositions.length; slot++) {
            if (this.rememberedPositions[slot] == position) {
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
        if (remember) {
            int slot = this.overwritten;
            this.overwritten = 1 - slot;
            this.rememberedPositions[slot] = position;
            this.rememberedChunks[slot] = chunk;
            this.rememberedOffsets[slot] = low;
        }
    }

    /** Keeps the remembered places where they are after the upper part of {@code chunk} from {@code kept} on moved out. */
    private void rememberSplit(int chunk, int kept) {
        for (int slot = 0; slot < this.rememberedPositions.length; slot++) {
            if (this.rememberedChunks[slot] > chunk) {
                this.rememberedChunks[slot]++;
            } else if (this.rememberedChunks[slot] == chunk && this.rememberedOffsets[slot] > kept) {
                this.rememberedChunks[slot]++;
                this.rememberedOffsets[slot] -= kept;
            }
        }
    }

    /** Keeps the remembered places where they are after {@code position} went in at {@code offset} of {@code chunk}. */
    private void rememberInsert(int position, int chunk, int offset) {
        for (int slot = 0; slot < this.rememberedPositions.length; slot++) {
            int remembered = this.rememberedPositions[slot];
            if (remembered >= 0 && remembered != position && this.rememberedChunks[slot] == chunk
                    && (this.rememberedOffsets[slot] > offset || this.rememberedOffsets[slot] == offset
                    && this.order.compare(position, remembered) < 0)) {
                this.rememberedOffsets[slot]++;
            }
        }
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
        System.arraycopy(this.chunks[chunk], kept, upper, 0, moved);
        System.arraycopy(this.prefixes[chunk], kept, upperPrefixes, 0, moved);
        this.chunks[chunk + 1] = upper;
        this.prefixes[chunk + 1] = upperPrefixes;
        if (this.walks) {
            int[] upperShared = new int[this.chunkCapacity];
            System.arraycopy(this.shared[chunk], kept, upperShared, 0, moved);
            this.shared[chunk + 1] = upperShared;
        }
        this.heads[chunk + 1] = upperPrefixes[0];
        this.counts[chunk + 1] = moved;
        this.counts[chunk] = kept;
        this.chunkCount++;
        if (this.ranked) {
            this.fenwick.rebuild(this.counts, this.chunkCount);
        }
    }

    private void requireWalks() {
        if (!this.walks) {
            throw new IllegalStateException("An index for ranks keeps nothing for walks");
        }
    }

    /** The tree of the chunk sizes, built the first time it is asked for and kept from then on. */
    private FenwickTree ranks() {
        if (!this.ranked) {
            this.fenwick.rebuild(this.counts, this.chunkCount);
            this.ranked = true;
        }
        return this.fenwick;
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
                this.rank = ChunkedContextIndex.this.ranks().sumBefore(this.chunk) + this.offset;
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
            ChunkedContextIndex.this.requireWalks();
            return Surroundings.sharedBits(ChunkedContextIndex.this.shared[this.chunk][this.offset]);
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
