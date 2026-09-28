# Architecture notes

Why the code is the way it is; constraints live in `CLAUDE.md`, the coders' rules in
`ALGORITHM.md`. Phase 9 of `TODO.md` extends this file.

## The dictionary index

The dictionary keeps the positions of a segment sorted by their contexts, and needs the rank of a
new position, the position at a rank, and the neighbours of a rank. `ContextIndex` is that
interface; `ChunkedContextIndex` is its one production implementation, a two-level sorted array
(chunks of 512 positions, split when full, with a Fenwick tree over the chunk sizes). A cursor
walks to the neighbouring ranks without a search, and the lookup that precedes an insert of the
same position hands its slot to the insert. The interface stays so that the brute-force oracle in
the tests, and any structure the performance harness wants to compare, can stand in for it.

### The thesis comparison of three structures

The thesis (§4.4, §5.2, Tables 5.4 and 5.5) compared the time and memory of three backing
structures for the same order-statistic operations: a red-black tree, an unbalanced binary search
tree and a sorted array with binary search. The dictionary never changed its output with the
structure, so the choice was only about speed. The measurements that led to the chunked array
were taken on book1 with `valach`, running the search-and-insert loop alone:

| Structure | `-d 6 -l 7` | `-d 10 -l 7` |
|---|---|---|
| Red-black tree of boxed `Integer`s, one `select` per candidate | 1,885 ms | 16,122 ms |
| Chunked sorted `int[]`, neighbour walk | 533 ms | - |
| The same with keys packed into two `long`s per position | 542 ms | 1,522 ms |
| Encoder only: final ranks presorted, Fenwick tree and bitset over rank space | 209 ms + 470 ms presort | 1,310 ms + 470 ms |

The chunked array was 3.5 times faster at the default window and 10 times at `-d 10`, works for
the decoder too, and uses four bytes per position instead of about sixty. Packing keys gained
nothing against a plain comparator over a byte array, and chunk sizes from 128 to 2,048 are within
15% of each other. The encoder-only structure is faster still but doubles the code that has to
agree, so it is kept in reserve. The unbalanced tree also overflowed the stack on a long run of
one byte, since equal contexts are ordered by position and the tree became a chain.
