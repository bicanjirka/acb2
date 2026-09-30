# Architecture notes

Why the code is the way it is. Constraints live in `CLAUDE.md`, the coders' rules in `ALGORITHM.md`,
the things left undone in `TODO.md`.

## The shape: a core that takes bytes and gives a stream

`Compressor` turns bytes into a `CompressedStream` and back, in memory. It touches no file and no
console, keeps no state between calls, and starts no thread: the segments of a stream are coded by an
`OrderedMapper` on whatever executor the caller gives it, which is why everything a segment is coded with
(its dictionary, its models, its funnels) is made for that segment. `ACBClient` and `ACBFileIO` only adapt
that to files, a block at a time, so a file's size is not bounded by the heap.

Two layers of choice sit under it. The settings of a stream, `CompressionSettings`, are what the header of
the container carries; a `TripletCoding` constant is the coder they name, and carries a `Coder`, which
makes the `SegmentCoding` that turns one segment into a block and back. There are two kinds of coder:

- The five thesis coders are layouts of triplet fields over a window of ranks. `triplets` parses what the
  dictionary finds into `Triplet`s, a pure `TripletLayout` turns them into fields and back, and an
  `EntropyCoding` (`coding`) turns the fields into bytes.
- `acb` has no layout: the probabilities of what it codes come from the funnel of analogies of each step,
  so `associative` codes the position, the length and the literal against distributions made at the step.

The `ACBProvider` makes the parts of a segment (dictionaries, writer, reader) from the settings, which is
the seam the tests use to look inside.

The packages depend on each other in one direction, upward: `counts` (the Fenwick tree) knows nothing;
`dictionary` uses it; `triplets` uses the dictionary; `coding` uses both; `associative` uses all of them;
and the root package, where `Compressor` and the settings are, uses everything. Two things go the other
way and are accepted. The packages above `counts` throw `format.MalformedStreamException`, since a stream
that cannot be decoded is found at the bottom as often as at the top, and `format` uses the models of
`coding` for the length frequencies of a header (`LengthFrequencies`). And `format` writes a header that names the coder and
entropy enums and the settings of the root, because the header is where those settings are defined on
the wire.

## One rule for both sides

The encoder and the decoder of a coder must agree on everything that is not sent: which entries the
dictionary holds, what a literal cannot be, which candidates a funnel has. Two coders in this project's
past did not: the thesis's LCP coder compared against text that its decoder did not have yet, so every
file it wrote was undecodable, and Buyanovsky's own `AC.C` fails to decode book1 at its default level.

So what a step does to the state is written once. For the thesis coders, `SegmentState.apply` is the only
place a triplet reaches the dictionary, and the encoder's parser and the decoder's layout both feed it.
For `acb`, `AssociativeSteps` is the whole step, and it asks a `SymbolPort` for each symbol: the encoder's
port finds it in the text and codes it, the decoder's reads it and rebuilds the text. A decision that only
one side could take has nowhere to be written.

## Blocks, and the versions of the container

Every segment is a block of its own: the dictionary and the models start empty in each. That costs a
little (0.03% on Calgary at 1 MB segments, 0.27% at 100 KB, `ALGORITHM.md` 4.3), and buys what matters
more: blocks are coded and decoded in parallel, a block that does not shrink is stored as it is (random
input grows by the few bytes of the container, where it once grew by 10.8%), and a damaged block damages
no other. The container is checked by a CRC-32 before anything in it is trusted, and every count in it is
bounded by what the stream has left before anything is allocated from it.

A change to the layout, to the codes of the coders, to how a coder lays out triplets or to what its models
code bumps `ContainerFormat.VERSION`, and a golden file per coder, checked in under
`src/test/resources/golden/v<VERSION>`, fails the build if it is forgotten.

| Version | What changed |
|---|---|
| 1 | The first container: self-describing and checked |
| 2 | The range coder of this project replaced a vendored arithmetic coder |
| 3 | Independent blocks, written from one shared update rule; a block is stored when coding does not shrink it |
| 4 | The context rank is the neighbour that agrees longer, as ExCom and Buyanovsky's Lemma 3 say |
| 5 | The `acb` coder; a literal leaves out every byte that a candidate would have matched with |

## The dictionary index

The dictionary keeps the positions of a segment sorted by their contexts, and needs the rank of a new
position, the position at a rank, and the neighbours of a context. `ContextIndex` is that interface and
`ChunkedContextIndex` its one production implementation: a two-level sorted array, chunks of 512 positions
split when full, with a Fenwick tree over the chunk sizes. The interface stays so that the brute-force
oracle in the tests, and any structure the performance harness wants to compare, can stand in for it.

Next to each position the index keeps the first eight bytes of its context as a number and the bytes that
the entry has in common with the one before it. The searches compare the numbers and read the text only
when two are equal, which is seldom: inserting every position of book1 at a context depth of 255, one
insert costs 0.34 microseconds with the prefixes kept and not used, 0.27 comparing four-byte prefixes first
and 0.24 with eight. The
shared bytes are what lets the funnel of `acb` walk outward from a context and know how far each entry
agrees with it without comparing bytes: of three entries in order, the first and the last have in common
as many as the fewest of the two neighbours do. Chunks of 256 and 512 cost the same (0.24 and 0.25), 128
and 1,024 a little more (0.27, 0.28), 2,048 and 64 much more (0.35, 0.43). Reading eight bytes at a time in
the comparator made `valach` compress 13% faster at a depth of 10 and 21% at a depth of 255.

The funnel of `acb` takes the entries around a context from the index as runs copied with `arraycopy`
(`Surroundings`, primitive arrays refilled at every step), finds the weights of each side in a loop of its
own and merges the sides after; choosing between the sides entry by entry cost a funnel of 62 candidates
0.96 microseconds and this 0.58, a lookup of the place of the context being 0.21 of it. Every insert, too,
is two thirds a lookup (0.165 of 0.23 microseconds), which is why the inserts of a step's 5.55 positions
are the largest single cost of a step of `acb`.

### The thesis comparison of three structures

The thesis (§4.4, §5.2, Tables 5.4 and 5.5) compared the time and memory of three backing structures for
the same order-statistic operations: a binary search symbol table (a sorted array that shifts), a binary
search tree and a red-black tree, all from a textbook library under the GPL. The dictionary never changed
its output with the structure, so the choice was only about speed. The measurements that led to the
chunked array were taken on book1 with `valach`, running the search-and-insert loop alone:

| Structure | `-d 6 -l 7` | `-d 10 -l 7` |
|---|---|---|
| Red-black tree of boxed `Integer`s, one `select` per candidate | 1,885 ms | 16,122 ms |
| Chunked sorted `int[]`, neighbour walk | 533 ms | - |
| The same with keys packed into two `long`s per position | 542 ms | 1,522 ms |
| Encoder only: final ranks presorted, Fenwick tree and bitset over rank space | 209 ms + 470 ms presort | 1,310 ms + 470 ms |

The chunked array was 3.5 times faster at the default window and 10 times at `-d 10`, works for the
decoder too, and uses four bytes per position instead of about sixty; replacing the trees took the
defaults from 0.34 to 1.0 MB/s on Calgary and `-d 10` from 0.06 to 0.59 at the time, with byte-for-byte
the same output, and removed the GPL code. Packing keys gained nothing against a plain comparator over a
byte array, and chunk sizes from 128 to 2,048 were within 15% of each other; what later made the keys
worth keeping was a prefix compared first and skipped over in the walk, not a packing of the whole
context. The encoder-only structure is faster still but doubles the code that has to agree, so it is kept
in reserve. The unbalanced tree also overflowed the stack on a long run of one byte, since equal contexts
are ordered by position and the tree became a chain.

## Entropy coding

The fields of a block go through one `RangeEncoder` (a 32-bit range renormalised a byte at a time, carry
propagated as in LZMA, totals up to 2^20), each against an `AdaptiveFrequencyModel` of its own (a Fenwick
tree of frequencies). The model adds 32 for every symbol seen and halves all frequencies when the total
would pass its limit: 2^16, or 256 per symbol for alphabets wider than 256. Both numbers are part of the
stream format. They were chosen on Calgary with `valach`, 7-bit lengths: an increment of 1 with no halving
(the behaviour of the coder this replaced) gave 981,910 bytes, increments of 16 to 32 with the 2^16 limit
gave 977,4xx, limits of 2^13 and 2^14 were worse (983,000), and at `-d 10` the 1,025-symbol distance
alphabet needed the wider limit (965,900 with 2^16 against 957,300). Widening the length field to 8 bits
gains 0.13% and no width gains more, so no escape code for long matches was added.

`acb` codes against distributions made at each step (`CumulativeTable`) from those models, the weights of
its funnel and its exclusions, through the same range coder.

## What the thesis measured, and what was redone

Table 5.8 of the thesis ranks the five triplet forms on Calgary. Summed over the corpus its columns are
1,042,166 (simple), 1,034,247 (salomon), 1,020,361 (salomon2), 1,003,484 (valach) and 991,013 bytes (LCP),
to the precision of its three-digit ratios: the longest-prefix coder best, and Salomon's flag ahead of the
plain triplet. It could not be kept.

The LCP coder of that code compared contents against text that its decoder did not have, so the dictionary
the decoder rebuilt diverged from the encoder's and none of the files it wrote could be decompressed; its
column measured the size of output that was not a compressed file. The code behind the other columns was
found, once every coder was run through round-trip tests, to mishandle unsigned literal bytes, empty
input and a Valach match cut back to nothing. Table 5.8 was therefore redone after every coder
round-tripped every corpus file at every segment size, with the rules of each written down
(`ALGORITHM.md`), at the settings of this project (`-d 6 -l 7`, 1 MB segments, 10 bytes of context):

| Coder | Thesis, bytes | Redone, bytes |
|---|---|---|
| `simple` | 1,042,166 | 991,407 |
| `salomon` | 1,034,247 | 1,008,201 |
| `salomon2` | 1,020,361 | 974,777 |
| `valach` | 1,003,484 | 974,670 |
| `lcp` | 991,013 | 994,471 |

`valach` and `salomon2` are the best of the five and the same to 0.01%; `lcp` is not the best, because the
smallest of the longest contents is often further from the context than the nearest one; and a match
without a literal (`salomon`) is the worst, not better than `simple`. The thesis's parameters are not
recorded, so the two columns are not the same experiment, and what the table is used for is the order.

Tables 5.4 and 5.5 (the structures) are superseded by the table above, which is of the structures that
replaced them; Tables 5.6 and 5.7 (the widths) by the ratio harness, which measures any width and length.

## The prefix variant

`lcp` (thesis section 3.3.1, [Bican]) sends the match length less the prefix the best content shares with the
second best, and needs the best to be the smallest of the longest, so that the prefix is below the length.
That is what makes it lose: it saves length bits and pays them back in distance, the smallest content being
far from the context. The master thesis of Adam Léhar [Léhar] (§3.5.1, "V1") has the same variant. By the
ratios of its Table 5.5 the mean over the 18 Calgary files of V1 is 0.5% below that of Valach's coder [Valach],
and the same rule in `valach`'s layout reproduces that here (970,320 bytes against 974,670, 0.45%). His
comparison with his own earlier implementation, which shows more, is against a baseline whose mean on the
same table is 4% above Valach's, so it does not measure the variant alone.

`prefix` takes the prefix from the walk instead of from the order of contents (`ALGORITHM.md` 5.6):
the best stays the nearest of the longest, and the prefix is what it shares with the candidates walked
before it, each of which matched fewer bytes. Calgary, `d = 6`, `l = 7`, ratio harness, one thread:

| | Bytes | Distance | Length | Literal | Compress / decompress |
|---|---|---|---|---|---|
| `valach` | 974,670 | 327,344 | 237,858 | 408,864 | 2.8 / 3.8 MB/s |
| `lcp`'s rule in `valach`'s layout (not kept) | 970,320 | 370,063 | 190,997 | 408,656 | 2.0 / 2.8 |
| `prefix` | 935,923 | 327,227 | 199,417 | 408,675 | 2.3 / 3.1 |
| `prefix`, `CONTEXT_ARITHMETIC` | 878,273 | | | | 1.4 / 1.7 |
| `acb`, `d = 6` | 844,144 | 341,584 | 149,131 | 352,801 | 1.5 / 1.65 |

It keeps the distance of `valach` and most of the saving in the length, and is smaller than `valach` on 13 of
the 14 files (`geo` is 0.8% larger). The length width hardly matters (`l = 7` to `9` is 0.1%), so the gain
is in the distribution of the length, not in matches beyond the longest. The decoder pays for it: it walks the
window up to the chosen candidate, so at `d = 12` it decodes far slower than `valach`, and a wider window
does not pay for that on the ratio. With the literal modelling of `CONTEXT_ARITHMETIC` the literals are as
cheap as `acb`'s, and the distance is cheaper (327 KB against 342 KB, at `acb`'s funnel of 6); the whole of
the remaining 4% is the length, 199 KB against 149 KB. `acb` codes it against the candidates that come
after the chosen one as well (`ALGORITHM.md` 7.5), and a layout coder's models cannot see the dictionary:
giving them that makes the coder `acb`. So the prefix variant is the best a layout can do, and the ratio of
`acb` is not reachable by it.

## Where the bits go

Book1 (768,771 bytes) with the thesis coder at its defaults, with `acb` at `-d 6` and with Buyanovsky's
`AC.C` (1 MB frame, `Kc 0`, its own build that prints the bits of each component):

| | `valach` | `acb` `-d 6` | `AC.C` |
|---|---|---|---|
| Steps | 171,089 | 173,755 | 128,844 |
| Average step | 4.5 bytes | 4.4 bytes | 6.0 bytes |
| Position or distance | 108,838 B | 113,582 B | 136 KB, over a funnel of about 1,000 candidates |
| Length | 67,819 B | 43,037 B | 36 KB, coded relative to better-ranked candidates |
| Literal | 108,198 B (order-0) | 83,792 B | 63 KB, with exclusion and funnel forecast |
| Total | 284,918 B | 240,482 B | 235 KB |

The gap to the thesis coders is structural, not tuning: real ACB spends more on the position but reaches
further, gets the length almost free from the neighbours, and rarely spends a full literal. Most of what
`acb` gains over `valach` is the length and the literal, and the rest of its gap to `AC.C` is the funnel:
32 candidates a side against about 1,000, which is also where `AC.C` spends its time.

## Speed over the phases

Compressing Calgary at the defaults, on the machine of the time: before the chunked index 0.45 MB/s
compressing and 0.8 decompressing, against 1.4 MB/s for ExCom in C++ with the same model; after the index
and the range coder 1.8 and 2.4; after the block format 1.7 and 3.5; with the comparator, prefixes and
shared bytes of the index about 2.5 and 3.5 (ratio harness, one thread, a faster machine than the first three).

## Measuring

Two harnesses live in `src/test/java/cz/cvut/fit/acb/harness`, and their Javadoc says how to run them. The
ratio harness compresses each file of a directory with the settings asked for, checks the round trip and
prints size, bits per character, speed and what each kind of field cost; the performance harness times
seeded inputs and fails under `PerformanceBudget`. Both are run before a phase is closed; a change that
improves a ratio lowers the pins of `RatioRegressionTest`, and a change that makes a coder faster raises the
floors. Times are from one machine and only compare within a table. The other compressors in the README
were run on the same corpus, the same machine and the same day, each file on its own as one process; this
project's rows run the files of a directory in one JVM: rebuild the jar, run `java -jar target/acb.jar
cal14 out [options]`, decompress to a second directory, compare every file, sum the sizes.

### Reproducing the reference rows

- **Calgary corpus:** `https://corpus.canterbury.ac.nz/resources/calgary.tar.gz`; the 14 classic files
  (`bib book1 book2 geo news obj1 obj2 paper1 paper2 pic progc progl progp trans`: drop paper3 to paper6),
  3,141,622 bytes. `gzip -9`, `bzip2 -9` and `xz -9` are run on each file.
- **ExCom** ([ExCom], Šimek's and Řezníček's library under the LGPL version 3, the source of `salomon2` and
  `valach` through its ACB module, which is Valach's [Valach]): the module is in
  `lib/method/acb`. Delete `#include <libio.h>` from `TripletCoder.hpp`, then build with
  `g++ -O2 -w -std=gnu++98 -fpermissive -DMETHOD_ACB -DMETHOD_ARITH -Iinclude -Ilib lib/*.cpp` and every
  `lib/method/**/*.cpp` except `dca/` and `ppm/`, then `src/app/main.cpp -lpthread`. Run `excom -m acb
  [-p d=N,l=N] -i in -o out`, and with `-d` to decompress.
- **`AC.C`** (Buyanovsky's 1994 code, from `http://ctxmodel.net/files/ACB.rar`, in `ACB/AC_SRC.RAR`, with
  his paper as `ACB/AC_ST_EN.DOC`): build it with GCC by replacing the externs of `AC_ASM.ASM` with C
  (`Dvc(A,B,C) = ((B+1)*A)/C - 1` and `Dvd(A,B,C) = ((A+1)*B - 1)/(C+1)` in 64 bits, `Log2int` and `Log2ie`
  by `__builtin_clz`, `GetBit` and `SetBit` as a bit toggle, `memmove4` as `memmove` of pointers), by
  rewriting the lvalue casts of `BitCmpLn` with `word *` locals and the pointer XOR swap of `StrFrc2` with a
  temporary, by dropping `_stack`, `_pascal` and the progress `printf`, and by allocating the output at
  `2n + 4096` bytes, zeroed. On Windows x64 `long` stays 32 bits, so the types are the original's. It is run
  as `ac c|d in out [Kc] [bNN]`; its exit code is 1 on success. The code carries no licence: read it, never
  copy it. At its default level (`Kc 2`) it fails to decode book1; `Kc 0` round-trips all 14 files.
  The widths of the table in `ALGORITHM.md` 7.9 (128, 55 and 16 a side: 839,769, 846,912 and 875,872 bytes)
  were measured on builds with the funnel capped.
- **The thesis** [Bican], for its tables: <http://hdl.handle.net/10467/68184>, the file
  `https://dspace.cvut.cz/server/api/core/bitstreams/233480f8-4ebc-4960-8764-a9e1ae51650a/content`.
  The code that produced them is in this repository's history before commit `6b30a11`; it overwrites its
  input, so run it on copies.
