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

## Development

```bash
mvn test                                        # round-trip tests over every settings combination
mvn test -Dtest=RoundTripTest#someSentenceName  # one test
```

Working constraints and code style are in `CLAUDE.md`. Known gaps and planned work are in
`TODO.md`.

## Licence

This is thesis work. Non-profit use is permitted under the terms in `acb-licence`.
It contains no third-party code.
