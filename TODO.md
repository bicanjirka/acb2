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
| 9 | Documentation pass and final measurements | small | no |

## Handoff for the next session

State on 2026-09-30: research, planning and phases 0 to 8 are finished (phase 8 ran before 7, so the
coder of phase 7 is written against segments coded in parallel). Phase 9 is open; do not begin one without
being asked. Do not redo the research below; its numbers are final unless the code
changes. Measure ratio and speed with the harnesses; their Javadoc in
`src/test/java/cz/cvut/fit/acb/harness` says how to run them.

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
| this, `-d 10 -ec CONTEXT_ARITHMETIC` | 925,593 | 2.36 | 4.1 s / 1.8 s * |
| this, `-ec CONTEXT_ARITHMETIC` | 934,267 | 2.38 | 1.9 s / 1.8 s * |
| ExCom ACB `d=10` | 967,714 | 2.46 | 6.0 s / 2.6 s |
| ExCom ACB `d=8` | 972,911 | 2.48 | 3.3 s / 3.0 s |
| this, defaults (`valach -d 6 -l 7`) | 974,670 | 2.48 | 1.6 s / 1.6 s * |
| ExCom ACB defaults (distance ±31, length 7 bits) | 988,420 | 2.52 | 2.3 s / 2.5 s |
| gzip -9 | 1,017,624 | 2.59 | 1.5 s |

\* Re-measured on 2026-09-30 on a faster machine, after phases 4 to 6 and 8 (segments coded in
parallel); every size in the other rows reproduced exactly there, and ExCom `d=10` took
2.8 s / 1.2 s and `AC.C` with the 1 MB frame 17.0 s / 18.0 s.

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

## Open gaps

### `acb` runs at 1.2 to 1.3 MB/s, not 2

Calgary at `d = 6`: 843,240 bytes (under the 850 KB target) at 1.3 MB/s compressing and 1.4
decompressing, one thread; `valach` does 2.3 and 3.1 on the same machine. A step of 5.55 bytes on
average costs about 4.2 us: the dictionary insert of each of its positions, 0.25 us (5.55 of them), the
funnel of analogies of the context, 1.0 us, and the funnel that votes on the literal, 1.0 us; the rest is
the coder and its tables (about 0.25 us of literal table). Even with no funnels at all the coder does
2.15 MB/s, the inserts and the literal table. Narrowing the funnel (`d = 5`: 855,407 bytes, about 1.7
MB/s) or the voting funnel (16 per side: +0.6% and 13% faster) buys speed with ratio, and inserting only
the first position of a step of 12 bytes or more, not 3 * log2 of the size, gives 841,798 bytes and about
3% speed.

- **Where:** `dictionary.FunnelWalk` and `ChunkedContextIndex.locate`, `around` and `insert`.
- **Approach:** make a funnel cost half: find the place of the next context from the place of the last
  one rather than by a search (the contexts of positions next to each other are related), and keep the
  byte each entry's content starts with next to it so that no candidate's text is read to find its match.
  The second, tried, gained nothing at the defaults. Or insert fewer positions per step, which the
  insert cost (0.25 us each) says is half of the time.

### Beyond `AC.C`: the paper's own coding and ACB 2.00's modelling

`AC.C` reaches 837 KB on Calgary and ACB 2.00 779 KB, and `acb` here 843 KB at `d = 6` and 835 KB at
`d = 8`. The paper codes a string by the candidate's number, a "difference bit" and an "extract" length,
which is the idea of the `lcp` coder done right; ACB 2.00 models the literals and positions further.

- **Where:** `associative` (a new step rule next to `AssociativeSteps`), `docs/ALGORITHM.md` section 7.
- **Approach:** as named variants, never changes to `acb`: the difference-bit and extract coding of the
  paper; a mixing of the funnel's literal votes with an order-1 model; a correction of the position
  weights by how often a candidate of that place won.

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
