# Known gaps and future work

The single place for outstanding design gaps and planned work; the source carries no inline
`TODO` notes. Closing an item deletes its entry in the same commit.

## Open gaps

### `acb` runs at 1.5 MB/s, not 2

Calgary at `d = 6`: 843,240 bytes (under the 850 KB target) at 1.5 MB/s compressing and 1.6 decompressing,
one thread (1.4 and 1.5 before its models dropped the Fenwick trees they never asked); `valach` does 2.5 and
3.5 on the same machine. Timed inside the encoder, a step (5.55 bytes on average) costs about 3.6 us: the
funnel of analogies of the context 0.7, the funnel that votes on the literal 0.6 and what is done with its
votes 0.3, the inserts of the step's positions 1.0 (0.18 each), the comparison of the candidates with the
text 0.5, the literal's distribution and coding 0.3, the length 0.2. Even with no funnels at all the coder
does 2.15 MB/s, which is the inserts and the literal. Narrowing the funnel buys speed with ratio: `d = 5`
gives 855,407 bytes at 1.8 MB/s and `d = 4` 878,363 at 2.1. Narrowing only the voting funnel to 16 a side
costs 0.6%, and inserting only the first position of a step of 12 bytes or more, not 3 * log2 of the size,
gives 841,798 bytes and about 3% speed.

- **Where:** `dictionary.FunnelWalk`, `ChunkedContextIndex.locate`, `around` and `insert`, and the loops
  over the funnel in `associative.AssociativeSteps` and `EncodingPort`.
- **Approach:** find the place of the next context from the place of the last one rather than by a search
  (the contexts of positions next to each other are related); keep the byte each entry's content starts
  with in the low byte of its prefix, so that no candidate's text is read to compare it or to take its
  vote (a separate array of those bytes was tried and gained nothing, the copy on every insert costing what
  the reads saved); insert with fewer moves of the arrays. Building a funnel in two passes, the weights
  and then the merge, took a funnel from 0.96 to 0.58 us, and giving a distribution's frequencies to a
  table one call at a time instead of setting an array was slower than two loops. About a quarter has to
  go to reach 2 MB/s, and none of these is worth more than a tenth.

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

### `acbx` runs at 0.9 MB/s

Calgary at `d = 6`: 734,658 bytes at about 0.9 MB/s both ways, one thread; `acb` does 1.5 and 1.6. A JFR profile
over Calgary puts two fifths of the time in the dictionary (the funnels, the voting funnel and the inserts, as in
`acb`), a sixth in the walk over the candidates (`Continuations.gather` and `keep`), a tenth in the counters, a
twelfth in the mixer and as much in setting up each literal's distributions. Of the 10.5 million decisions,
1.7 million are on a byte every live candidate agrees on; they take about 6% of the time and come in runs of one
to three bytes mostly, so coding the runs as lengths, a format change, would buy about 3%. Keeping each
counter's probability and count in one `int` gave 4%, one pass over the bytes for a literal's three
distributions 2%, and mixing two banks in one pass 3%; keeping the mixer's banks in one array, and remembering
each candidate's byte in `gather` so that `keep` reads no text, gave nothing.

- **Where:** the dictionary, as in the `acb` entry above; `associative.Continuations`; the hashed tables of
  `MatchEndModel`, `ContinuationModel` and `MixedLiteralModel`.
- **Approach:** the dictionary work of the `acb` entry serves both coders and is the most of what is left; after
  it, the cache misses of the hashed tables, which smaller tables or two counters to a cache line would cut, at
  a change of the format. `d = 5` is 10% faster for 0.3%.
