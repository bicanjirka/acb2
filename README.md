# ACB — Associative Coder of Buyanovsky

A lossless compressor built for a master's thesis at CTU FIT. It is based on
George Buyanovsky's associative coding. The input is split into segments, and each
segment is walked with a sorted dictionary of *contexts* (the bytes before a position) and
*contents* (the bytes after it). Every step emits a triplet that names a content by its distance
in the dictionary from the current context, together with a match length and, if needed, a literal
byte. An entropy coder then turns the triplets into bytes.

## Build

Needs Java 26 and Maven.

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
from the file. The only option that applies when decompressing is `-ds`.

| Option | Meaning | Default |
|---|---|---|
| `-de`, `--decompress` | Decompress instead of compress | compress |
| `-f`, `--force` | Overwrite output files that already exist | refuse |
| `-d N`, `--distance N` | Bits (1 to 16) for the distance field; max distance is 2^(N−1) | 6 |
| `-l N`, `--length N` | Bits (1 to 16) for the length field; max length is 2^N − 1 | 7 |
| `-tc C`, `--triplet-coder C` | Triplet layout: `simple`, `salomon`, `salomon2`, `valach` | `valach` |
| `-ds S`, `--dict-struct S` | Dictionary tree: `red_black`, `bst`, `st` | `red_black` |
| `-bs`, `--bit-stream-array` | Write triplet fields as plain bits instead of arithmetic coding | arithmetic |
| `-af F`, `--arith-freq F` | Initial arithmetic-coder frequencies for lengths, comma-separated | all 1 |
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
- **lcp** is experimental. The CLI refuses it because its output doesn't decompress yet (see
  `TODO.md`).

### Dictionary structures

All three are order-statistic trees that yield the same ranks, so the choice changes only speed.
Your choice when decompressing doesn't have to match the one used for compression.
`red_black` is a red-black BST, `bst` is an unbalanced BST, and `st` is a sorted-array symbol
table searched by binary search.

## Development

```bash
mvn test                                        # round-trip tests over every settings combination
mvn test -Dtest=RoundTripTest#someSentenceName  # one test
```

Working constraints and code style are in `CLAUDE.md`. Known gaps and planned work are in
`TODO.md`.

## Licence

This is thesis work. Non-profit use is permitted under the terms in `acb-licence`.
`nayuki.arithcode` is vendored from Project Nayuki under the MIT licence (see
`Readme-arith-coding.markdown`). `cz.cvut.fit.acb.dictionary.core` is adapted from Sedgewick and
Wayne's *Algorithms* (algs4), which is distributed under the GPL-3; it is due to be replaced (see
`TODO.md`).
