# Known gaps and future work

The single place for outstanding design gaps and planned work; the source carries no inline
`TODO` notes. Closing an item deletes its entry in the same commit.

## Open gaps

### `acb` runs at 1.2 to 1.3 MB/s, not 2

Calgary at `d = 6`: 843,240 bytes (under the 850 KB target) at 1.3 MB/s compressing and 1.4
decompressing, one thread; `valach` does 2.8 and 3.9 on the same machine. A step of 5.55 bytes on
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
