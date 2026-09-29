# Known gaps and future work

The single place for outstanding design gaps and planned work; the source carries no inline
`TODO` notes. Closing an item deletes its entry in the same commit, and a finished phase deletes
its heading.

The goal: a clean, fast, well-tested ACB compressor whose every coder is exactly the algorithm
it is named after, plus a faithful implementation of Buyanovsky's own associative coder, which
is where the compression ratio is. Phases run in order; each is committed on its own, stays
green, and is measured with the ratio and performance harnesses before and after. The cheap phases come
first so the big refactor (phase 7) lands on a tested, measured, fast base.

| Phase | Theme | Size | Format change |
|---|---|---|---|
| 6 | Thesis coders made faithful: context reference, LCP, literal model | medium | `VERSION` 4 |
| 7 | Buyanovsky's associative coder (`-tc acb`) | large | `VERSION` 5 (new coder code) |
| 8 | Parallel segments and streaming I/O | medium | no |
| 9 | Documentation pass and final measurements | small | no |

## Handoff for the next session

State on 2026-09-29: research, planning and phases 0 to 5 are finished. The user's order for the
rest is phases 6, 7 and 9, with phase 8 (large) left for later. Start with phase 6, first entry.
Do not redo the research below; its numbers are final unless the code changes. Measure ratio and
speed with the harnesses; their Javadoc in `src/test/java/cz/cvut/fit/acb/harness` says how to run
them.

- **Decided by the user:** stay on Log4j 2 (no SLF4J/Logback, unlike jTD). Coders follow their
  source on what defines the algorithm (fields, layout, context and content rules);
  implementation is free; where a source is silent or buggy the chosen rule goes into
  `docs/ALGORITHM.md`; improvements are named variants, never silent changes to a faithful coder.
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

### Profile after phase 3

JFR on book1, `valach -l 7` (80 samples compressing, 60 decompressing): the chunked index is
about 43% of the time either way (`locate`, the Fenwick update), the context comparator 5-10%,
and Nayuki's coder 14% compressing and 30% decompressing, mostly rebuilding its cumulative table
(replaced in phase 4).

### Other findings

- **Throughput.** Before phase 3: about 0.45 MB/s compressing and 0.8 MB/s decompressing at the
  defaults, against ExCom's 1.4 MB/s in C++ with the same model. After phases 3 and 4: 1.8 and 2.4 MB/s;
  after the block format of phase 5: 1.7 and 3.5.

## Phase 6: thesis coders made faithful

### Literals after a miss exclude nothing

`CONTEXT_ARITHMETIC` codes a literal against the model of the byte before it and leaves out the
byte the chosen content continues with when the match ended on a mismatch. That took the Calgary
literals from 408,864 to 368,471 bytes (`valach`, 4.1% of the file). A literal after no match at all
can also never equal the first byte of any candidate of the window, and `AC.C` excludes every
candidate that matched as long as the best one; it codes book1's literals in 3.9 bits against 5.05
for the order-0 model.

- **Where:** `triplets.LiteralTracker`, `triplets.LiteralContext`, `coding.LiteralModel`.
- **Approach:** let the context carry a set of excluded bytes instead of one, filled from the
  window's candidates by both sides, and measure what it gains against what it costs in speed. The
  funnel forecast belongs to phase 7.

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

- **Where:** new `-tc acb` constant of `TripletCoding` (phase 5 made a coder one constant), its parser and model in the
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

Each segment already becomes a block of its own, with a dictionary and entropy models that start
empty, but the blocks are compressed and decoded one after the other.

- **Where:** `Compressor.compress`, `ACBFileIO.SegmentReader`.
- **Approach:** compress segments on a bounded executor and write
  the blocks in order; decode blocks in parallel the same way. The core stays free of threads it
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
