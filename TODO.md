# Known gaps and future work

The single place for outstanding design gaps and planned work; the source carries no inline
`TODO` notes. Closing an item deletes its entry in the same commit, and a finished phase deletes
its heading.

The goal: a clean, fast, well-tested ACB compressor whose every coder is exactly the algorithm
it is named after, plus a faithful implementation of Buyanovsky's own associative coder, which
is where the compression ratio is. Phases run in order; each is committed on its own, stays
green, and is measured with the harness from phase 0 before and after. The cheap phases come
first so the big refactors (phases 5 and 7) land on a tested, measured, fast base.

| Phase | Theme | Size | Format change |
|---|---|---|---|
| 0 | Safety net: tooling, tests, harnesses, the coder specification | medium | no |
| 1 | Correctness and hardening, CLI and logging hygiene | small | no |
| 2 | Encoder-only ratio wins | small | no |
| 3 | Dictionary engine: primitive, cache-friendly, licence-clean | medium | no |
| 4 | Own range coder and adaptive models; drop the vendored Nayuki code | medium | `VERSION` 2 |
| 5 | Encoder/decoder symmetry refactor and container v2 | large | `VERSION` 3 |
| 6 | Thesis coders made faithful: context reference, LCP, literal model | medium | `VERSION` 4 |
| 7 | Buyanovsky's associative coder (`-tc acb`) | large | `VERSION` 5 (new coder code) |
| 8 | Parallel segments and streaming I/O | medium | no |
| 9 | Documentation pass and final measurements | small | no |

## Handoff for the next session

State on 2026-09-28: research and planning are finished; phase 0 is under way (tooling and dead
code, the coder specification and the test gaps done; next the two harnesses). Do not redo the
research below; its numbers are final unless the code changes.

- **Decided by the user:** stay on Log4j 2 (no SLF4J/Logback, unlike jTD); remove every
  `@author` tag (phase 0, dead code entry); in phase 3 keep the `ContextIndex` seam but remove
  the `-ds` option. Coders follow their source on what defines the algorithm (fields,
  layout, context and content rules); implementation is free; where a source is silent or buggy
  the chosen rule goes into `docs/ALGORITHM.md`; improvements are named variants, never silent
  changes to a faithful coder.
- **Research artifacts** (optional, outside the repo; the recipes below rebuild everything):
  `C:\Users\juras\dev\acb-research` holds `cal/cal14` (the corpus), `bin/excom.exe` (ExCom),
  `bin/ac.exe` and `bin/acs.exe` (`AC.C` ported, and a build that prints bits per component to
  stderr), `acport/` (`port.py` and `instr.py` turn the original `AC.C` into those builds),
  `proto/DictBench.java` (the dictionary prototypes: `javac -cp acb.jar`, run as
  `DictBench <file> rb|chunk|chunkpacked|offline <dBits> <lBits> [chunkSize]`),
  `bench_java.sh <jar> [options]`, `bench_excom.sh <exe> "<d=N,l=N>"` and
  `bench_ac.sh <exe> [Kc] [bNN]` (each compresses cal14, decompresses, verifies, and prints the
  total and times), `ref/` (Buyanovsky's archive, his paper as text, the thesis as text). The
  `.exe` files need the MinGW `bin` directory on `PATH`.

## Research findings

### Reference setup

Everything below was measured on this machine on 2026-09-28. Scratch files live in a
session-only directory; to rebuild them:

- **Calgary corpus:** `https://corpus.canterbury.ac.nz/resources/calgary.tar.gz`; the 14
  classic files (drop paper3-6, 3,141,622 bytes), copied read-only.
- **C++ compiler:** WinLibs MinGW-w64 GCC 16.1 at
  `%LOCALAPPDATA%\Microsoft\WinGet\Packages\BrechtSanders.WinLibs.POSIX.UCRT_Microsoft.Winget.Source_8wekyb3d8bbwe\mingw64\bin`
  (installed, not on `PATH`; prepend it in the shell).
- **ExCom:** `C:\Users\juras\Downloads\excom.tar`, ACB module in `excom.git/lib/method/acb`.
  Build it by deleting `#include <libio.h>` from `TripletCoder.hpp`, then
  `g++ -O2 -w -std=gnu++98 -fpermissive -DMETHOD_ACB -DMETHOD_ARITH -Iinclude -Ilib lib/*.cpp
  <every lib/method/**/*.cpp except dca/ and ppm/> src/app/main.cpp -lpthread`; run with
  `excom -m acb [-p d=N,l=N] -i in -o out` and `-d` to decompress.
- **Buyanovsky's source:** `http://ctxmodel.net/files/ACB.rar` (extract with
  `C:\Program Files\7-Zip\7z.exe x`), then `ACB/AC_SRC.RAR` holds `AC.C` and `AC_ASM.ASM`.
  `ACB/AC_ST_EN.DOC` (RTF) is his English paper "Research method of pseudostochastic systems"
  with the algorithm; `TECHNIQ.TXT` is the comparison article. To build `AC.C` with GCC:
  replace the `AC_ASM.ASM` externs with C (`Dvc(A,B,C) = ((B+1)*A)/C - 1` and
  `Dvd(A,B,C) = ((A+1)*B - 1)/(C+1)` in 64-bit, `Log2int`/`Log2ie` via `__builtin_clz`,
  `GetBit`/`SetBit` (a bit toggle), `memmove4` as `memmove` of pointers); rewrite the lvalue casts in
  `BitCmpLn` with `word *` locals and the pointer XOR-swap in `StrFrc2` with a temporary; drop
  `_stack`, `_pascal` and the progress `printf`; allocate the output buffer at `2n + 4096`, zeroed.
  On Windows x64 `long` stays 32-bit, so the types match the original. `main` returns 1 on success.
  Usage: `ac c|d in out [Kc] [bNN]`. The code carries no licence: read it, never copy it.
- **Thesis:** `https://dspace.cvut.cz/server/api/core/bitstreams/233480f8-4ebc-4960-8764-a9e1ae51650a/content`,
  text via `pypdf`. Key parts: §1.2 the worked `mississippi` example, §1.3 Salomon's
  modifications, §2 prior implementations, §3.3.1 the LCP method, §5 Tables 5.4-5.8.
- **Thesis-era code:** `git worktree add --detach <dir> 6b30a11^`, pom source/target 17,
  `mvn -o compile`, run with `java -cp "<dir>/target/classes;target/acb.jar"
  cz.cvut.fit.acb.ACBClient`. It overwrites its input file: throwaway copies only.
- **Benchmark method:** rebuild the jar from HEAD first (`mvn -q clean package -DskipTests`), run
  directory mode (`java -jar target/acb.jar cal14 out [options]`) in one JVM, decompress to a
  second directory, compare every file, sum sizes.

### Where ACB stands

Calgary, 14 files, 3,141,622 bytes; every row round-tripped unless noted. ExCom and `AC.C`
start one process per file, and this coder runs one JVM for the directory.

| Compressor | Bytes | bpc | Compress / decompress |
|---|---|---|---|
| ACB 2.00a (Mahoney's table, calgary.tar, older 2 GHz machine) | 778,760 | 1.98 | 18.7 s / 18.7 s |
| bzip2 -9 | 828,347 | 2.11 | 2.0 s |
| Buyanovsky `AC.C` 1994, `Kc 0`, 1 MB frame | 837,107 | 2.13 | 29.9 s / 32.7 s |
| `AC.C`, funnel capped at 128 per side | 839,769 | 2.14 | 26.6 s / 32.8 s |
| xz -9 | 845,952 | 2.15 | 2.7 s |
| `AC.C`, funnel capped at 55 per side (ACB 1.17 "NORMAL" width) | 846,912 | 2.16 | 26.4 s / 27.0 s |
| `AC.C`, `Kc 0`, its default 256 KB frame | 849,813 | 2.16 | 13.8 s / 15.9 s |
| `AC.C`, funnel capped at 16 per side (ACB 1.17 "FAST" width) | 875,872 | 2.23 | 29.1 s / 28.2 s |
| ExCom ACB `d=10` | 967,714 | 2.46 | 6.0 s / 2.6 s |
| ExCom ACB `d=8` | 972,911 | 2.48 | 3.3 s / 3.0 s |
| ExCom ACB defaults (distance ±31, length 7 bits) | 988,420 | 2.52 | 2.3 s / 2.5 s |
| this, `-tc valach -d 6 -l 7` | 1,012,317 | 2.58 | 7.1 s / 3.9 s |
| gzip -9 | 1,017,624 | 2.59 | 1.5 s |
| this, `-tc valach` | 1,049,160 | 2.67 | 7.1 s / 3.8 s |
| this, defaults (`simple`) | 1,076,271 | 2.74 | 7.2 s / 3.9 s |

Measured earlier on patched builds (phase 2 below): nearest tie with the rank-0 fix gives
`valach -d 10 -l 6` 962,297 and `valach -d 8 -l 5` 985,371.

`AC.C` at its default level (`Kc 2`) fails to decode book1 (a crash in the decoder, same with
exact and carry-emulating division), so the 1994 demo has an encoder/decoder asymmetry
somewhere; `Kc 0` round-trips all 14 files. A reimplementation must derive every model decision
from state both sides share, which the symmetry refactor in phase 5 makes structural.

Where the bits go on book1 (768,771 bytes):

| | This coder, `valach` | `AC.C`, 1 MB frame |
|---|---|---|
| Steps | 171,328 triplets | 128,844 |
| Average match | 3.7 bytes | 5.1 bytes |
| Position / distance | 117 KB (5.73 of 6 bits) | 136 KB (8.4 bits, over a funnel of ~1,000 candidates) |
| Length | 68 KB (3.2 bits) | 36 KB (2.3 bits, coded relative to better-ranked candidates) |
| Literal | 108 KB (5.05 bits, order-0) | 63 KB (3.9 bits, with exclusion and funnel forecast) |
| Total | 293 KB | 235 KB |

The gap is structural, not tuning: real ACB spends more on the position but reaches further,
gets length almost free from the neighbours, and rarely spends a full literal.

### What real ACB does, and how the thesis coders relate to their sources

Moved to `docs/ALGORITHM.md`: section 7 describes Buyanovsky's coder, and the deviations of each
current coder from its source are listed with its rules in sections 2 to 5.

### Profile of the current code

JFR on book1, `valach -l 7`. Compression: `RedBlackBST.select` 50%, `RedBlackBST.put` (with
the inlined 10-byte comparator) 33%, rebuilding Nayuki's cumulative table 6%. Decompression:
`put` 60%, `rank` 16%, the arithmetic decoder about 13%. The order-statistic tree is about 85%
of the time either way; the entropy coder is next.

### Dictionary structure prototypes

A scratch benchmark ran the `valach` encoder's search/insert loop on book1 over four
structures; all produced the identical triplet sequence.

| Structure | `-d 6 -l 7` | `-d 10 -l 7` |
|---|---|---|
| Current: algs4 red-black tree of boxed `Integer`s, `select` per candidate | 1,885 ms | 16,122 ms |
| Chunked sorted `int[]` (two-level B+ tree, 512 per chunk, Fenwick over chunk sizes), neighbour walk | 533 ms | - |
| The same with keys packed into two `long`s per position | 542 ms | 1,522 ms |
| Encoder-only: final ranks presorted, Fenwick tree and bitset over rank space | 209 ms + 470 ms presort | 1,310 ms + 470 ms |

The chunked array is 3.5x faster at the default window and 10x faster at `-d 10`, works for the
decoder too, and uses 4 bytes per position instead of about 60. Packing keys gains nothing
against a plain comparator over a byte array. The encoder-only structure is faster still but
doubles the code that must agree; keep it in reserve. Chunk sizes from 128 to 2,048 are within
15% of each other.

### Other findings

- **Licences.** `dictionary.core` (`BST`, `RedBlackBST`, `BinarySearchST`, about 1,800 lines) is
  algs4 code under GPL-3, which cannot ship under `acb-licence` (non-profit only; GPL forbids
  added restrictions) and is not declared anywhere. `coding.RangeCoding` is LGPL-3 (dead code).
  `coding.ArithmeticCoding` was copied from a Google Code project with no stated licence (dead
  code). The Nayuki files carry no header; their MIT notice lives only in
  `Readme-arith-coding.markdown`, so that file is required for exactly as long as `nayuki.arithcode`
  ships, and goes with it in phase 4.
- **`-ds bst` crashes.** 200 KB of one repeated byte: `StackOverflowError`. Equal contexts are
  ordered by position, so the unbalanced tree becomes a chain and the recursive `put` overflows.
  It escapes `main`'s `catch (Exception)`.
- **Incompressible input expands.** 2 MB of random bytes grow by 10.8% and take 13.9 s.
- **Throughput.** About 0.45 MB/s compressing and 0.8 MB/s decompressing at the defaults; ExCom is
  3x faster in C++ with the same model, and a Java coder with the structures above should match it.
- **Tests.** The suite (45 s under `mvn verify`, all green) covers only the default bit widths,
  a corpus of five files under 500 bytes, and never checks ratio or speed. Nothing tests the order-
  statistic trees, the comparator, or a stream written by an older build.

## Phase 0: safety net

### Compression-ratio harness on a standard corpus

The thesis-relevant number (ratio per coder, dictionary and entropy coder) has no reproducible
harness, and no test guards it: a refactor that halves compression passes every test. Every
phase below is measured with it.

- **Where:** a `harness` source set (or test scope) with `RatioHarness`; a `CompressionStats`
  record returned by `Compressor` beside the stream; a ratio-regression test.
- **Approach:** run every settings combination over a corpus directory given on the command
  line (Calgary, Canterbury), verify each round trip while measuring, and print ratio, bpc,
  throughput, and bits per field (distance, length, literal, flag) as a table beside gzip, bzip2,
  xz, ExCom and `AC.C` from the table above. The per-field accounting comes from
  `CompressionStats` (the summed code lengths the entropy coder reports), which replaces the
  core's debug logging of triplet counts. Pin the ratio of each combination on the test corpus
  with a small tolerance, so a regression fails the build.

### Performance harness and budget

Nothing measures speed, and no test covers a large input.

- **Where:** `PerformanceHarness` beside `RatioHarness`.
- **Approach:** compress and decompress a fixed seeded corpus (text-like, binary, runs, random)
  several times after warm-up, report MB/s per direction and coder, and exit non-zero when over
  an explicit budget. Start the budget at today's numbers and tighten it as phases 3-5 land.
  JMH only if the plain harness proves too noisy.

## Phase 1: correctness and hardening

### Header bit widths allow allocating gigabytes

The header accepts bit widths up to `CompressionSettings.MAX_FIELD_BITS` (30), and the adaptive
arithmetic model allocates `2^bits + 1` ints, plus as many again for the cumulative table.
Compressing with `-d 30` fails with `OutOfMemoryError` at a 512 MB heap. The decoder builds the
same model from the header, so a crafted file of about 30 bytes with a recomputed CRC should do
the same. This breaks the rule that decoding bounds every count before allocating. Wide fields
are not useful anyway: `-d 24` compressed a 3.5 KB text to 98% of its size. The same holds for
the length frequencies: any count and any positive value is accepted, and a sum beyond the
model's limit fails with `IllegalArgumentException` instead of `MalformedStreamException`.

- **Where:** `CompressionSettings.MAX_FIELD_BITS`, `format.ContainerFormat.bits` and the
  frequency loop, `coding.AdaptiveArithmeticCompress`/`AdaptiveArithmeticDecompress`.
- **Approach:** cap the widths at 16 (ExCom caps at 12) in the settings and therefore in
  decoding; bound the frequency count by the length alphabet and each value by what the model
  can total. Tests: a header with a wider field or oversized frequencies is rejected as
  malformed.

### Bad payloads fail with arbitrary exceptions or garbage

A well-formed container with a corrupt payload is not caught: an out-of-range rank gives an NPE
or `IndexOutOfBoundsException`, not `MalformedStreamException`. A payload with fewer arrays than
fields fails the same way. `AdaptiveArithmeticDecompress` swallows an `IOException` and returns
symbol 0, so decoding continues on invented data, and reads past an end-of-stream symbol keep
decoding. `ValachTripletCoder.decodeStep` casts a `-1` end-of-stream read straight to a byte.
Tests corrupt only the container, never the payload.

- **Where:** `Compressor.decompress`, every `decodeStep` in `triplets.coder`,
  `dictionary.DictionaryBase.copy`/`select`, `coding.ByteToTripletConverter.read`,
  `coding.AdaptiveArithmeticDecompress`.
- **Approach:** validate distances and ranks against the dictionary size in the decoder and
  throw `MalformedStreamException`; stop at the first `-1`. Add a jqwik property that feeds
  random payloads inside a valid container through `Compressor.decompress` and accepts only
  success or `MalformedStreamException`.

### Swallowed I/O errors in the coding layer

`printStackTrace` followed by carrying on, against the error rule in `CLAUDE.md`, in seven
places. In the encoder a swallowed failure produces a corrupt `.acb` without any error.

- **Where:** `coding.AdaptiveArithmeticCompress` (`compress`, `terminate`),
  `coding.AdaptiveArithmeticDecompress` (constructors, `decompress`),
  `coding.BitArrayComposer.compress`, `coding.BitArrayDecomposer.decompress`.
- **Approach:** these are in-memory streams, so rethrow as `UncheckedIOException` on the
  encoder side and as `MalformedStreamException` on the decoder side. Delete the
  `noPrintStackTrace` suppression in `checkstyle.xml` in the same commit.

### CLI and logging hygiene

The CLI logs to standard output, where `-m` writes its measurements. Every error is printed
twice (once by `System.err`, once by the logger). A decompression that fails half-way leaves a
partial output file. The help path exits 0 even for a usage error. `ACBClient` fills mutable
instance fields while parsing, keeps `Options` static, and reconfigures Log4j as a side effect of
parsing. The core logs per triplet through lambdas that allocate on every call even when tracing
is off, and `CLAUDE.md` names Log4j 2 while the user's other projects use SLF4J with Logback.

- **Where:** `ACBClient`, `ACBFileIO`, `src/main/resources/log4j2.xml`, `pom.xml`, the coders'
  `LOG.trace` calls, `CLAUDE.md`.
- **Approach:**
  - Parse into an immutable `CliRequest` record (paths, mode, measure target, settings, log
    level) and apply the log level in `run`.
  - Log to standard error only; print a failure once, as one line, and the stack trace only at
    `DEBUG`; exit 0 for success and `-h`, 1 for a failure, 2 for bad usage.
  - Write each output to a temporary sibling and move it into place when complete, so a failure
    leaves nothing behind; refuse to overwrite an existing output unless `-f` is given.
  - Drop the per-triplet trace logging from the core; `CompressionStats` (phase 0) and the test
    fixture `TripletLog` cover inspection. Keep one `DEBUG` line per stream.
  - Logging stack: stay on Log4j 2 (the user's decision). The core uses `log4j-api` only;
    `log4j-core` is touched only by the CLI, for setting the level.

### Documentation errors

`README.md` names the vendored package `cz.cvut.fit.acb.nayuki.arithcode`; it is
`nayuki.arithcode`. It says nothing about the algs4 code.

- **Where:** `README.md`.
- **Approach:** fix the package name now; the licence section is rewritten when phases 3 and 4
  remove the third-party code.

## Phase 2: encoder-only ratio wins

Each changes only the encoder's choices; files carry their widths, so old files still decode
and `VERSION` stays. Measure each on Calgary with round trips verified, and commit separately.

### The default length field is too narrow

The default 4-bit length caps a match at 15 bytes; ExCom defaults to 7 bits (127). With
`-tc valach -l 7` Calgary shrinks from 1,049,160 to 1,012,317 bytes (-3.5%) at the same speed:
pic 75,937 to 60,259 (-21%), trans 23,257 to 20,100 (-14%), progl 19,608 to 17,996 (-8%),
book1 unchanged. The distance default (`-d 6`) already matches ExCom's ±31.

- **Where:** `CompressionSettings.DEFAULTS` and its default length frequencies, the `-l` help
  text in `ACBClient`, `README.md`.
- **Approach:** make 7 the default length width, re-tune the initial length frequencies for the
  wider alphabet (the current ones model `AC.C`'s excess length, not a raw length), and pick the
  defaults with the ratio harness rather than by hand; also make `valach` the default layout,
  which the thesis and ExCom both found best. Longer term, code lengths beyond a cutoff with an
  escape (for example Elias-gamma) so the cap disappears.

### Ties pick the farthest match, and rank 0 is never a candidate

The most frequent distances on book1 are 31, 30, 29, 28: the far edge of the window.
`searchContent` keeps the first candidate of a given length (strict `>`) and scans from the
farthest rank, while Broukhis's description picks the nearest, and ExCom walks outward from the
context so the nearest wins by construction. Separately, `searchContent` scans from `lo + 1`, so
when `lo` is clamped to 0, rank 0 is never a candidate although its distance fits. Both fixes
together, measured on Calgary: `simple` 1,040,155 (-3.4%), `valach` 1,021,875 (-2.6%),
`valach -d 8 -l 5` 985,371 (-3.7%), `valach -d 10 -l 6` 962,297 (-4.6%), all round-tripping.

- **Where:** `dictionary.DictionaryBase.searchContent`, `dictionary.DictionaryLCP.searchContent`.
- **Approach:** walk outward from the context as ExCom does (distance 0, -1, +1, -2, ...), so
  the first longest match is the nearest and the walk can stop at `maxLength`; scan from `lo` and
  skip only candidates with `ctx - i > maxDistance - 1`. Tests: among equal matches the nearest is
  chosen; rank 0 is found. The LCP coder needs the opposite tie rule (phase 6), so the rule
  belongs to the coder, not the dictionary. Delete the matching deviations from
  `docs/ALGORITHM.md` section 3.5 and update its worked examples.

## Phase 3: dictionary engine

### Replace the algs4 trees with one primitive order-statistic structure

The three trees are the hot spot (85% of the time), the source of the `bst` crash (a 50 KB run of
one byte overflows the stack; the tests skip that case in
`SettingsCombination.knownRoundTripDefect(DegenerateInput)`, and the exclusion goes with the
trees), GPL-3 code in a non-profit-licensed project, and boxed: every position is an `Integer`, every comparison goes
through `Comparator<Integer>`, every candidate costs an O(log n) `select`. `OrderStatisticTree`
inherits a 16-method `Serializable` interface and uses five methods.

- **Where:** `dictionary.core` (replaced), `dictionary.DictionaryBase`, `dictionary.DictionaryLCP`,
  `DictionaryStructure`, `CompressionSettings`, `ACBProviderImpl`, `ACBClient` (`-ds`), `README.md`.
- **Approach:** one `ContextIndex` over `int` positions: a chunked sorted array (a two-level
  B+ tree: chunks of about 512 positions, split when full, a Fenwick tree over chunk sizes for
  `rank` and `select`), with a cursor that walks neighbours in both directions without
  `select`. `insert` reuses the slot the preceding search found for the same position, so the
  first context of every step costs one descent, not two. Compare contexts over a padded
  `byte[]` (sentinel bytes before the segment remove the bounds checks). Delete `BST`,
  `RedBlackBST`, `BinarySearchST`, `OrderStatisticTree` and `BinarySearchTree`, and the `-da`
  flag for them in the surefire `argLine`. Test the index
  against a brute-force sorted-list oracle with jqwik (rank, select, neighbours after random
  inserts, including equal contexts). Target from the prototype: book1's search loop under 0.6 s
  at `-d 6`, under 1.6 s at `-d 10`.
- **`-ds` (decided by the user: keep the seam, drop the menu):** it existed for the thesis's
  experiment (§4.4, §5.2, Tables 5.4 and 5.5: time and memory of three backing structures), never
  changed the output, and the tests ran every structure only to prove that. `ContextIndex` stays
  an interface with one production implementation. The brute-force oracle lives in tests, and
  a second fast structure (the encoder-only presorted Fenwick, or a plain `int[]` with
  `arraycopy` as the thesis's `BinarySearchST` baseline) goes in the performance harness if a
  comparison is wanted. Remove `DictionaryStructure`, `-ds`, and that dimension of
  `SettingsCombination`, and record the thesis comparison in `docs/ARCHITECTURE.md`.

### Byte comparisons through `ByteSequence`

Every byte of every match goes through `byteAt` with bounds checks, `copy` allocates a
`ByteBuilder` and arrays per triplet, and the encoder and decoder see different
`ByteSequence` types, which the coders tell apart by downcasting.

- **Where:** `dictionary.ByteSequence`, `ByteArray`, `ByteBuilder`, `DictionaryBase.match`/`copy`,
  every coder.
- **Approach:** one `SegmentBuffer` (a padded `byte[]` with a fill mark) for both sides. Forward
  matching uses `Arrays.mismatch` (vectorised in the JDK); a decoder copy is one `arraycopy`, or a
  byte loop when the match overlaps its own output. Related faults go with it: `ByteBuilder.clone`
  copies the whole capacity, `ByteBuilder.equals` compares contents while `hashCode` hashes
  array identity, and `ByteArray.array()` returns its internal array.

## Phase 4: entropy coding

### Frequency table costs O(alphabet) per symbol and never rescales

`nayuki.arithcode.SimpleFrequencyTable.increment` drops the cumulative table, and the next
`getLow` rebuilds it, so every coded symbol costs O(alphabet) time and allocates a new array.
On a 3.5 KB text, `-d 16` took 169 ms against 31 ms at the defaults, and the ratio went from
0.58 to 0.74. Frequencies are never halved and one model per field lives for the whole stream,
so after about 2^30 symbols in a field Nayuki's `MAX_TOTAL` check throws and large inputs fail
to compress. Nayuki's coder also emits one bit at a time and checks invariants on every symbol
(it is a teaching implementation, by its own readme). The field streams are looked up in a
`HashMap<Integer, ...>` per symbol.

- **Where:** `coding` (new), `nayuki.arithcode` and `Readme-arith-coding.markdown` (deleted),
  `README.md` licence section, `src/test/java/nayuki`.
- **Approach:** our own byte-oriented range coder (32-bit range, carry propagation as in LZMA,
  totals up to 2^16) taking `(cumulative, frequency, total)`, so it also serves the per-step
  distributions of phase 7; an `AdaptiveFrequencyModel` with periodic halving (ExCom halves at
  2^14; measure the limit), a linear cumulative scan for small alphabets and a Fenwick tree for
  large ones; adaptive binary models for flags. Fields index an array, not a map. Verify the
  coder against a reference model on random distributions, then delete the vendored package, its
  tests and its readme together. Bumps `VERSION`.

### The bit-array writer does not fit the per-field template

`BitArrayComposer.getArray` returns `null` for all fields but one, gated by a `doReturn` flag,
and `TripletToByteConverter.finish` has to skip the nulls. The bit-array writer is not per-field
at all.

- **Where:** `coding.BitArrayComposer`, `coding.BitArrayDecomposer`, `coding.TripletToByteConverter`.
- **Approach:** a separate implementation of the field sink (phase 5) that writes one bit stream.
  Its only use is measuring the entropy coder's gain; keep it for that, as a small class.

## Phase 5: encoder/decoder symmetry and container v2

The big refactor, on a tested and measured base. Container changes are batched into one
`VERSION` bump.

### One update rule, shared by both sides

Each coder class is both encoder and decoder, and its mode depends on the runtime type of its
`ByteSequence`: every `decodeStep` downcasts to `ByteBuilder`. `encodeStep` and `decodeStep`
each restate the dictionary update and the end-of-segment shortening of a match (for example
`ValachTripletCoder` lines 41-53 against 85-90). That duplication is where the LCP defect lives,
and where `AC.C`'s own decoder went wrong.

- **Where:** `triplets.coder` (all coders), `triplets.TripletSupplier`, `utils.TripletUtils`,
  `dictionary.DictionaryInfo`.
- **Approach:** a sealed `Triplet` (`Literal`, `Match`, `MatchWithLiteral`); per coder a pure
  layout (triplet to field symbols and back) and an encoder-only parser (search result to
  `Triplet`); one `apply(Triplet, SegmentState)` that updates dictionary and position, used by
  both sides. `DictionaryInfo` becomes a sealed `NoMatch | Match` record instead of a `-1`
  content. Measure the allocation cost with the harness rather than assume it.

### Split the two-way triplet processor

`TripletProcessor` has `read`, `write`, `getSize` and `setSize`; the writer throws on the read
half and the reader on the write half. The segment size travels through this field channel and
lands in `payload[0]`, which `Compressor.decompress` then has to validate. `Compressor.compress`
documents that every segment but the last has the first one's size, and never checks it.

- **Where:** `triplets.TripletProcessor`, `coding.TripletWriter`,
  `coding.TripletToByteConverter`, `coding.ByteToTripletConverter`, `Compressor`,
  `format.StreamHeader`.
- **Approach:** a field sink and a field source as separate interfaces; the segment size moves
  into `StreamHeader`; a segment of the wrong size is rejected.

### Split the dictionary by audience

`Dictionary` mixes encoder methods (`search`, `searchContent`), decoder methods (`copy`,
`select`) and test-only members (`clone`, and `equals`/`hashCode`, used only by
`fixtures.DictionarySnapshots`), and exposes rank arithmetic.

- **Where:** `dictionary.Dictionary`, `dictionary.DictionaryBase`, `fixtures.DictionarySnapshots`.
- **Approach:** narrow encoder and decoder views over the `ContextIndex` from phase 3; snapshots
  built by the test harness from public queries instead of `clone`/`equals` on production
  classes.

### Record the length; drop the end-of-stream sentinels

The end of the stream is signalled three ways: `-1` from field reads, `Integer.MAX_VALUE` from
`decodeStep`, and `TripletCoder.DecodeFlag`. Every arithmetic field stream also spends an EOF
symbol, which is why its alphabet is `2^n + 1`.

- **Where:** `format.ContainerFormat`, `format.StreamHeader`, `triplets.coder.BaseTripletCoder`,
  `Compressor.decompress`, `coding`.
- **Approach:** store the original length in the header and let the decoder loop by count. The
  sentinels, `DecodeFlag` and the EOF symbols go away. All fields of a segment go interleaved into
  one range-coded stream, in the order the decoder reads them (as ExCom does), which drops the
  per-field length prefixes and makes the payload streamable.

### Per-segment blocks and a stored fallback

Payload arrays are per field and span the whole file, the payload is held as one
`List<byte[]>`, and `ContainerFormat` reads the whole file. Segmenting bounds the dictionary but
not memory, and the entropy model shared across segments blocks parallel compression. Random
input grows by 10.8%.

- **Where:** `format.ContainerFormat`, `Compressor`, `ACBFileIO`.
- **Approach:** one block per segment (raw length, coded length, then the coded bytes, or the raw
  bytes when coding did not help), streamed in and out. Decide deliberately whether the entropy
  model resets per segment and measure what the reset costs in ratio.

### Constants the decoder depends on are not part of the format

`Dictionary.ReverseIndexComparator.MAGIC_CONST = 10` sorts contexts by their last 10 bytes
only, compared as signed bytes. That decides the ranks, so changing it silently breaks every
existing file, yet it is neither in the header nor tied to `ContainerFormat.VERSION`. The same
holds for the policy that the dictionary resets per segment while the entropy model lives for the
whole stream.

- **Where:** `dictionary.Dictionary.ReverseIndexComparator`, `Compressor`, `format.ContainerFormat`.
- **Approach:** make the context depth a setting stored in the header, compare unsigned (the
  byte order every source assumes, and the one "lexicographically smaller" in the LCP
  specification means), and name the remaining policies as format constants beside `VERSION`.
  Measure what a longer depth does to ratio and speed; ACB treats it as the main control.

### Coder choice lives in several switches

Choosing a triplet coding picks both the dictionary and the coder in two `switch`es in
`ACBProviderImpl`, and a third table in `ContainerFormat` maps it to a code. The exhaustive
switches do force every case to be handled, so this is low priority.

- **Where:** `ACBProviderImpl`, `TripletCoding`, `format.ContainerFormat`.
- **Approach:** a `CoderScheme` value carrying its parser, layout and format code, so a new coder
  (phase 7) is added in one place. `CompressionSettings` gets a narrow factory and stops
  restating seven components in every `withX`; `lengthFrequencies` becomes an immutable value
  instead of a defensively copied `int[]` with a hand-written `equals`.

## Phase 6: thesis coders made faithful

### The context reference is always the predecessor

`searchContext` returns `rank - 1`, the context sorted just below the current one. The thesis
example takes the greater index; ExCom and Buyanovsky's Lemma 3 take whichever neighbour agrees
with the current context longer, which puts the best contents nearer the reference and shrinks
distances. Decoding repeats the choice, so it changes the format.

- **Where:** the context lookup in `dictionary`, `docs/ALGORITHM.md`.
- **Approach:** take the neighbour with the longer backward match (ties to the predecessor),
  measure on Calgary together with the nearest-tie walk. Bumps `VERSION`.

### LCP dictionary diverges between encoder and decoder

The decoder's dictionary differs from the encoder's after a few updates, and segmented
decoding calls `select` with a negative rank. `ACBClient` refuses `-tc lcp` until this is
closed, because the files it wrote did not decompress. The thesis-era build, rebuilt and run on
Calgary: LCP output decompresses for 0 of the 10 text files, and every coder crashes on the 4
binary files (the signed-byte bug fixed in `32d5071`), so Table 5.8's LCP and binary-file numbers
came from output that was never decoded. What is known:

- `DictionaryLCP.searchContent` mixes ranks and text positions: `bestIdx` holds a rank, but the
  equal-length branch assigns it `cnt` (a position) and compares `bestIdx + bestLen` as a
  position.
- It picks the second best by match length against the text, which the decoder cannot see, and
  the decoder searches from the best content's position with the best itself still a candidate.
- The thesis specification (§3.3.1) is sound: best = the lexicographically smallest content of
  maximal match length L; second = a lexicographically smaller one; send `L - lcp(best, second)`,
  which is never negative or zero. It misses one rule: comparisons may only read bytes the decoder
  already has, so order and LCP are computed on bytes before the current index, and a content
  truncated there counts as smaller.

- **Where:** `dictionary.DictionaryLCP`, `triplets.coder.LCPTripletCoder`, the `-tc` check in
  `ACBClient`, `SettingsCombination.knownDictionaryDefect`.
- **Approach:** implement §3.3.1 exactly, with the visibility rule, efficiently. The encoder
  already knows every candidate's match length m(c) from the window scan. A candidate that
  matched less than L sorts below the best exactly when its byte at m(c) is below the text's byte
  there (or it is truncated), and then its LCP with the best is m(c). So
  `lcp = max m(c)` over those candidates, one byte comparison each, with no extra scan. Only
  the tied candidates at L need a comparison beyond L to find the smallest. The decoder, knowing only
  the best, takes the largest LCP with the best among the candidates that sort below it: one
  `Arrays.mismatch` per candidate, and only when the sent length is non-zero. The length cap then
  applies to `L - lcp`, which lets matches exceed `2^bits - 1`, the point of the method. Remove
  the CLI refusal and the test exclusion together, and re-measure Table 5.8 with round trips
  verified.

### Literals are coded order-0

Every triplet ends in a literal coded by one order-0 model. On book1 an order-1 model would take
the literals from 108 KB to about 94 KB, and order-2 to about 84 KB (a static, optimistic
estimate). A literal after a match that stopped on a mismatch can never equal the byte the
chosen content continues with, yet the model still reserves probability for it. `AC.C` codes
book1's literals in 3.9 bits against 5.05 here, by exclusion and funnel forecast.

- **Where:** the literal field of each coder, `coding`.
- **Approach:** exclude the byte the chosen content predicts (and, as in `AC.C`, the bytes of
  every candidate that matched as long), and context the literal model on the previous byte
  with a fallback while contexts are young. Named as a deviation for the thesis coders in
  `docs/ALGORITHM.md` if kept on by default. Bumps `VERSION`.

### Fixed segments instead of an adaptive dictionary lifetime

Each 1 MB segment starts from an empty dictionary, however well the old one was doing. ExCom
keeps one dictionary up to 2^20 contexts, stops inserting when full, and clears it when the
compression ratio degrades by 0.1% (checked every 512 bytes once 2^18 contexts are in).
ExCom bases that decision on `bitsWritten()` in the encoder and `bitsRead()` in the decoder,
which differ for an arithmetic coder (the decoder reads ahead), so its encoder and decoder can
clear at different points. Do not copy that. `AC.C` instead replaces the weaker neighbour once
its frame is full.

- **Where:** `Compressor`, `CompressionSettings.segmentSize`, `dictionary`.
- **Approach:** measure against a policy both sides compute from shared model state only (for
  example the summed code length of coded symbols, not I/O bit counts), or keep fixed segments
  if the gain is small: fixed segments are what make parallel compression possible.

## Phase 7: Buyanovsky's associative coder

### Real ACB as a coder of its own

The triplet coders are Salomon's simplification. Buyanovsky's own coder (`docs/ALGORITHM.md`,
section 7) is 14% smaller than ExCom and 17% smaller than this coder's best on Calgary, and even at the
narrowest funnel (16 per side) it beats ExCom's best by 9%. A fixed window of ±2^(d-1) ranks
cannot get there: ACB admits only contexts that agree with the current one beyond the noise
level, weights them by that agreement, and codes the position by those weights. This
supersedes the earlier "candidate set is a fixed window" entry.

- **Where:** new `-tc acb` coder scheme (`CoderScheme` from phase 5), its parser and model in the
  core, `docs/ALGORITHM.md` (the specification), the harness.
- **Approach:** implement the model from the specification, not from `AC.C` (which has no
  licence), on the phase 3-5 base:
  - The funnel: walk outward from the context slot with the `ContextIndex` cursor while the
    bit-level context agreement exceeds `log2(Kc·N/500)`, compared eight bytes at a time as
    big-endian `long`s with `numberOfLeadingZeros` of the XOR. Cap the width with a setting in the
    header, with presets at ACB 1.17's S = 16, 55 and 100.
  - Position coding with weights from agreement and rank distance plus an adaptive escape, by
    the phase 4 range coder from a per-step cumulative table.
  - Length as the excess over the best better-weighted candidate's LCP, with the boost from
    later candidates; the literal only after a mismatch, with exclusions and the funnel forecast.
  - The frame: one dictionary per segment, the neighbour-replacement policy when full, and the
    long-match insertion skip; measure each against simply keeping everything.
  - Round trips on Calgary for every width and level, since the 1994 code itself loses symmetry
    at `Kc 2`. Targets: at most 850 KB on Calgary, at least 2 MB/s each way.
  - Afterwards, as research: Buyanovsky's own difference-bit-and-extract coding of the paper, and
    modern modelling (mixing the funnel's literal predictions, secondary estimation) toward ACB
    2.00's 779 KB.

## Phase 8: throughput and scale

### Parallel segment compression

Each segment already gets its own dictionary, but segments are compressed sequentially, and the
entropy model shared across the stream ties them together.

- **Where:** `Compressor.compress`, `ACBFileIO.SegmentReader`.
- **Approach:** after per-segment blocks, compress segments on a bounded executor and write
  them in order; decode segments in parallel the same way. The core stays free of threads it
  does not own: the executor is passed in.

### Streaming instead of whole files in memory

`ContainerFormat.decode` reads the whole compressed file and `Compressor.decompress(byte[])`
builds the whole output, so file size is bounded by the heap and by 2 GB arrays.

- **Where:** `ACBFileIO`, `format.ContainerFormat`, `Compressor`.
- **Approach:** read and write block by block through channels; keep the in-memory
  `byte[]` API as a convenience over the streaming one. Test with a generated input above 2 GB
  in the performance harness, not in the unit suite.

## Phase 9: documentation and final measurements

### Project documentation

There is no rationale document, and the README has to follow every phase above.

- **Where:** `docs/ARCHITECTURE.md`, `docs/ALGORITHM.md`, `README.md`, `CLAUDE.md`.
- **Approach:** `docs/ARCHITECTURE.md` for the why: the headless core and its one-way package
  dependencies, the shared update rule, why one dictionary structure, the format's versioning,
  the history of the thesis measurements and why Table 5.8 was redone. The README gets the final
  harness table (every coder beside gzip, bzip2, xz, ExCom and `AC.C`), the options that exist,
  and a licence section without third-party code. `CLAUDE.md` changes only where a constraint
  changed (package list, logging stack, the removed `-ds`).
