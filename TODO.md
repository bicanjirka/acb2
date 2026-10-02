# Known gaps and future work

The single place for outstanding design gaps and planned work; the source carries no inline
`TODO` notes. Closing an item deletes its entry in the same commit.

## Open gaps

### Beyond `acbx`: the paper's bit-level coding and ACB 2.00's modelling

`acbx` codes a step as the paper places the text among the contents of the funnel, a byte at a time, and
reaches 735 KB on Calgary, under the 779 KB given for ACB 2.00a. The paper does it a bit at a time: a step
ends on the bit where the text leaves the candidates, the "difference bit" is implied, and the "extract"
counts only the bits where it could differ. What ACB 2.00 models beyond `AC.C` is not known.

- **Where:** `associative` (a step rule next to `MixedSteps`), `dictionary` (contexts that end inside a byte),
  `docs/ALGORITHM.md` section 9.
- **Approach:** as a named variant, never a change to `acb` or `acbx`: a dictionary of bit positions, which the
  byte-ordered `ChunkedContextIndex` cannot hold as it is; measure first what the bits inside a literal cost
  `acbx` against what the bit-level walk would spend on them.

### `acbx` runs at 1.05 MB/s

Calgary at `d = 6`: 734,658 bytes at about 1.05 MB/s compressing and 1.1 decompressing, one thread; `acb` does
2.25 and 2.3. A JFR profile over Calgary puts a quarter of the time in the mixer and its counters, a quarter in the
walk over the candidates (`Continuations.gather`, which reads the text of every live candidate at every byte, and
`keep`), a quarter in the dictionary (the lookups and inserts of the index, the funnels and the votes) and a
sixteenth in setting up each literal's distributions. The decisions over Calgary: 582,466
escapes (the end at depth 0), 1,655,261 ends on a byte every live candidate agrees on (79 KB of output), 895,688
ends where they differ (71 KB), 2,730,494 continuations (242 KB) and 4,655,690 bits of 590,090 literals. The
agreeing ends take about 6% of the time and come in runs of one to three bytes mostly (of about 470,000 runs,
five in six are under four bytes), so coding the runs as lengths, a format change, would buy about 3%. Keeping each
counter's probability and count in one `int` gave 4%, one pass over the bytes for a literal's three
distributions 2%, and mixing two banks in one pass 3%; keeping the mixer's banks in one array, and remembering
each candidate's byte in `gather` so that `keep` reads no text, gave nothing. Not yet tried: a literal still
walks its 256 bytes twice, once in `MixedLiteralModel.fillAllowed` and once when its distribution sums them for
the first cumulative sum asked for; the sums could be the running sums of the counts plus the few votes, with
`CumulativeTable.seal`'s halving (when the total passes 2^20, which a base total of at most 2^16 reaches only with
very heavy votes) kept exactly as it is, since the output must not change.

- **Where:** `associative.Continuations`; the hashed tables of `MatchEndModel`, `ContinuationModel` and
  `MixedLiteralModel`; `dictionary.ChunkedContextIndex.locate` and `insert`.
- **Approach:** a lookup in the index costs about 0.18 microseconds on book1 and most of it is waiting for memory,
  so the places of the positions a step inserts could be looked up together, their searches interleaved so that
  the processor fetches for several at once, and the place of a position inside a match could be found from that
  of the matching position of the chosen candidate, which is next to it; `gather` could take a candidate's first
  byte from the funnel at depth 0. After that, the cache misses of the hashed tables, which smaller tables or two
  counters to a cache line would cut, at a change of the format. `d = 5` is 10% faster for 0.3%. Measure every
  step with `harness.SpeedComparison`; the other harnesses cannot see a change of a few percent.

### How much ACB 2.00 gains from coding Calgary as one tar

The `ACB 2.00a` row is Mahoney's figure for `calgary.tar`, the 14 files as one file; every other row codes them
apart. On the same table bzip2 and gzip lose on the tar and LZMA gains 2.5%, and ACB, whose frame is large,
would gain too, by an amount no source gives (`docs/REFERENCES.md`, [Mahoney-DCE]).

- **Where:** `README.md` ("How it compares"), `harness.ReferenceResults`, `docs/REFERENCES.md`.
- **Approach:** run `ref/ACB/200C/ACB.EXE` from the research folder (2.00c; no 2.00a is there) in DOSBox on the
  14 files apart and on `calgary.tar`, and give both, saying which version was run.
