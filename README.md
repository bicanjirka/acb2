# ACB — Associative Coder of Buyanovsky

A lossless compressor built for a master's thesis at CTU FIT. It is based on
George Buyanovsky's associative coding. The input is split into segments, and each
segment is walked with a sorted dictionary of *contexts* (the bytes before a position) and
*contents* (the bytes after it). Every step emits a triplet that names a content by its distance
in the dictionary from the current context, together with a match length and, if needed, a literal
byte. An entropy coder then turns the triplets of a segment into a block of bytes, and a segment
that does not shrink is stored as it is. Every block is independent of the others.

## Build

Needs Java 25 and Maven.

```bash
mvn package            # tests + shaded jar at target/acb.jar
mvn verify             # also writes a coverage report to target/site/jacoco
```

## Usage

```bash
java -jar target/acb.jar input output [options]        # compress
java -jar target/acb.jar input.acb output -de          # decompress
```

`input` can be a file or a directory. For a directory, every regular file in it is processed
into the same file name inside the `output` directory. An output file that already exists is
refused unless you pass `-f`, and an output is written to a temporary file and moved into place,
so a failure leaves neither a partial file nor a replaced one.

Diagnostics go to standard error; help and measurements go to standard output. The exit code is
0 on success (and for `-h`), 1 when the work failed (a missing input, a corrupt stream), and 2
when the arguments were not understood.

A compressed file records the coding settings it was written with, so decompression reads them
from the file; only `-f`, `-j`, `-m` and `-log` apply when decompressing.

Files are read, coded and written one segment (1 MB) at a time, so a file's size is not bounded by
the heap. Segments are independent, so `-j` codes several at once on both sides and the compressed
file is the same for any number of threads.

| Option | Meaning | Default |
|---|---|---|
| `-de`, `--decompress` | Decompress instead of compress | compress |
| `-f`, `--force` | Overwrite output files that already exist | refuse |
| `-d N`, `--distance N` | Bits (1 to 16) for the distance field; max distance is 2^(N−1). For `acb`, the funnel of analogies takes 2^(N−1) entries from each side of a context | 6 |
| `-l N`, `--length N` | Bits (1 to 16) for the length field; max length is 2^N − 1 | 7 (`acb`: 8) |
| `-tc C`, `--triplet-coder C` | Coder: `simple`, `salomon`, `salomon2`, `valach`, `lcp`, or `acb` | `valach` |
| `-cd N`, `--context-depth N` | Bytes (1 to 255) of context that order the dictionary | 10 (`acb`: 255) |
| `-bs`, `--bit-stream-array` | Write triplet fields as plain bits instead of range coding (not for `acb`) | range coding |
| `-ec C`, `--entropy-coder C` | `ADAPTIVE_ARITHMETIC`, `BIT_ARRAY`, or `CONTEXT_ARITHMETIC`, which models literals by the byte before them and leaves out the bytes that would have made the match longer, and is about 6% smaller (`acb` codes with models of its own and takes only the first) | `ADAPTIVE_ARITHMETIC` |
| `-af F`, `--arith-freq F` | Initial range-coder frequencies for lengths (each coded length adds 32), comma-separated | all 1 |
| `-j N`, `--threads N` | Threads that code segments at the same time; the output does not depend on it | the number of processors |
| `-m [out]`, `--measure [out]` | Print time, sizes and ratio per file to `out` or stdout | off |
| `-log L`, `--log-level L` | Log4j level (`INFO`, `DEBUG`, `TRACE`, …) | `WARN` |
| `-h`, `--help` | Print help | |

### Triplet coders

- **simple**: always writes `(distance, length, next byte)`.
- **salomon**: a flag bit, followed by `(literal)` or `(distance, length)`.
- **salomon2**: like `salomon`, but a match also carries the next byte, as
  `(distance, length, next byte)`.
- **valach**: no flag. Length 0 means `(literal)`; otherwise it writes
  `(length, distance, next byte)`.
- **lcp**: laid out as `simple`, but the best match is the lexicographically smallest of the longest ones, and the length sent is the match length less the prefix it shares with the contents below it, which the decoder works out itself.
- **acb**: Buyanovsky's own coder. Each step gathers the *funnel of analogies* of the context, the entries of
  the dictionary whose contexts agree with it beyond chance, weighted by how far they agree and how near
  they lie. It codes which of them continues the text (or none) by its weight, the length of the match
  as what it has beyond the matches of the candidates weighed higher, and, when the match ended on a
  mismatch, the literal, which cannot be a byte that a candidate would have matched with, and which the
  funnel of the context after the match votes on. `docs/ALGORITHM.md` section 7 has every rule.

## How it compares

The 14 classic files of the Calgary corpus (3,141,622 bytes), each compressed on its own and the sizes
added up, every row decompressed and compared. Times are wall-clock seconds on one machine, 2026-09-30;
the other compressors are started once per file, this project's rows run the files in one JVM
(`bench_java.sh`), so they include the start of the JVM and its warm-up. The `ACB 2.00a` row is from
Mahoney's published table, on an older machine.

| Compressor | Bytes | bits/byte | Compress / decompress |
|---|---|---|---|
| ACB 2.00a (Mahoney's table) | 778,760 | 1.98 | 18.7 s / 18.7 s |
| bzip2 -9 | 828,347 | 2.11 | 0.5 s / 1.2 s |
| **`-tc acb -d 8`** | 835,696 | 2.13 | 4.3 s / 4.0 s |
| Buyanovsky's `AC.C` (1994), `Kc 0`, 1 MB frame | 837,107 | 2.13 | 18.9 s / 20.3 s |
| **`-tc acb -d 7`** | 837,618 | 2.13 | 3.4 s / 3.2 s |
| **`-tc acb`** (`-d 6`) | 843,240 | 2.15 | 2.9 s / 2.7 s |
| xz -9 | 845,952 | 2.15 | 1.0 s / 0.8 s |
| `AC.C`, `Kc 0`, its default 256 KB frame | 849,813 | 2.16 | 8.0 s / 9.0 s |
| **`-tc acb -d 5`** | 855,407 | 2.18 | 2.4 s / 2.4 s |
| `-tc valach -ec CONTEXT_ARITHMETIC -d 10` | 912,571 | 2.32 | 6.2 s / 3.7 s |
| `-tc valach -ec CONTEXT_ARITHMETIC` | 916,945 | 2.33 | 2.5 s / 2.0 s |
| `-tc salomon2 -ec CONTEXT_ARITHMETIC` | 917,050 | 2.34 | 2.4 s / 2.2 s |
| `-tc simple -ec CONTEXT_ARITHMETIC` | 933,678 | 2.38 | 2.4 s / 2.1 s |
| `-tc lcp -ec CONTEXT_ARITHMETIC` | 936,816 | 2.39 | 3.4 s / 2.8 s |
| ExCom `acb` `d=10` | 967,714 | 2.46 | 3.2 s / 1.4 s |
| ExCom `acb` `d=8` | 972,911 | 2.48 | 2.0 s / 1.3 s |
| `-tc valach` (the default) | 974,670 | 2.48 | 1.7 s / 1.3 s |
| `-tc salomon2` | 974,777 | 2.48 | 1.7 s / 1.3 s |
| ExCom `acb` defaults | 988,420 | 2.52 | 1.5 s / 1.3 s |
| `-tc simple` | 991,407 | 2.52 | 1.6 s / 1.2 s |
| `-tc salomon -ec CONTEXT_ARITHMETIC` | 993,980 | 2.53 | 2.0 s / 1.6 s |
| `-tc lcp` | 994,471 | 2.53 | 2.2 s / 1.7 s |
| `-tc salomon` | 1,008,201 | 2.57 | 1.8 s / 1.3 s |
| gzip -9 | 1,017,624 | 2.59 | 0.4 s / 0.7 s |

`acb` is Buyanovsky's coder and is where the ratio is: at `-d 8` it beats `AC.C` with its widest funnel and
the 1 MB frame, and at `-d 6` it is under `xz -9`, at a sixth of the time of `AC.C`. The five coders
of the thesis are Salomon's and Valach's layouts of a triplet; the best of them at the defaults is 16%
larger than `acb` (9% with the literal modelling of `CONTEXT_ARITHMETIC`). In the ratio harness one thread
codes `acb` at `-d 6` at about 1.4 MB/s and decodes it at 1.5, and `valach` at 2.5 and 3.5; a file of several
segments is coded on all the processors (`-j`). `docs/ALGORITHM.md` has the rules of every coder and
`docs/ARCHITECTURE.md` why the code is as it is and what was measured to get there.

## Development

```bash
mvn test                                        # round-trip tests over every settings combination
mvn test -Dtest=RoundTripTest#someSentenceName  # one test
mvn verify                                      # the quick check: tests, formatting, style, coverage
mvn verify -Pfull                               # also the tests tagged slow

mvn -q package -DskipTests
java -cp "target/acb.jar;target/test-classes" cz.cvut.fit.acb.harness.RatioHarness DIR [key=a,b ...]
java -cp "target/acb.jar;target/test-classes" cz.cvut.fit.acb.harness.PerformanceHarness
```

The ratio harness compresses a directory (the Calgary corpus, say) with each coder and setting asked for,
checks every file round-trips and prints size, speed and what each field cost; the performance harness
fails under the floors of `PerformanceBudget`. (Use `:` for `;` outside Windows.)

Working constraints and code style are in `CLAUDE.md`. Known gaps and planned work are in
`TODO.md`.

## Licence

This is thesis work. Non-profit use is permitted under the terms in `acb-licence`.
It contains no third-party code: the coders implement published algorithms (Salomon's, Valach's and
Buyanovsky's) from their descriptions, and `acb` was written from his paper and a reading of his 1994
reference code, which carries no licence and is not copied. The reference compressors in the table above
were run, not included.
