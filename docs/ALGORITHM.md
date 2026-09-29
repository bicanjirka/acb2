# The coders, specified

What each coder does, rule by rule, so that "the same as the original" has something written to be
compared against. Tests and reviews cite a rule by its section number (`ALGORITHM.md §3.2`). This
is a specification of the current code, not a history: where a rule differs from its source, the
difference is listed as a **Deviation**, and it is deleted from here in the commit that removes it.

Sources: the CTU FIT master thesis this project accompanies (§1.2 basic method, §1.3 Salomon's
modifications, §3.2.1 triplet variations, §3.3.1 second best content), Salomon's *Data
Compression* (as the thesis describes it), Valach's ExCom (`salomon2`, `valach`), and
Buyanovsky's own 1994 code and paper (section 7).

## 1. Terms

- A **segment** is the unit that is coded on its own (`CompressionSettings.segmentSize`, default
  1,000,000 bytes). Positions are counted from 0 within it.
- The **context** of position `p` is the text before it, `s[p-1], s[p-2], ...`; its **content** is
  the text from `p` on.
- The **dictionary** holds one entry per already coded position. An entry's **rank** is its
  0-based place in the dictionary order (section 2).
- `D = 2^(distanceBits - 1)` is the reach of the search window in ranks; `L = 2^lengthBits - 1`
  is the longest match a triplet can carry.

## 2. The dictionary

**2.1 Entries.** After every step the entries are exactly the positions `0 .. idx-1` of the
segment, where `idx` is the position coding continues from. A step that codes `n` bytes inserts
positions `idx .. idx+n-1`, whether or not it carries a literal. Each segment starts with an
empty dictionary.

**2.2 Order.** Entries are ordered by context, compared right to left over at most the last `C`
bytes, where `C` is the context depth (`CompressionSettings.contextDepth`, default 10, stored in
the stream header). Entries `p` and `q` compare `s[p-k]` with `s[q-k]` for `k = 1 .. min(p, q, C)`;
the first difference decides, as **unsigned** bytes. If none differs, the smaller position sorts
first. So a context that runs out of bytes sorts before every longer one it is a prefix of, and
among equal contexts the latest position has the highest rank.

Everything in this order reads only bytes before the position, so the decoder rebuilds the same
dictionary from the bytes it has decoded.

**2.3 Deviations.**
- The order looks at `C` bytes of context where the thesis compares the whole context. Over
  Calgary (`valach`, `d = 6`, `l = 7`) a depth beyond 8 changes the size by under 0.1%: 976,600
  bytes at 8, 976,751 at 10, 976,364 at 255, while decompression slows by a third. With a wider
  window (`d = 10`, `l = 6`) it is 960,171 at 8 and 959,289 at 255. The default stays at 10.

## 3. Searching

**3.1 The current context.** For the position `idx` about to be coded, the two entries between
which `idx` would be inserted are its neighbours, the *predecessor* below and the *successor*
above. `ctx` is the rank of the neighbour whose context agrees longer with the context of `idx`,
counting equal bytes going back from `idx`, at most `C` of them (2.2); on a tie it is the
predecessor. With only one neighbour it is that one, and with an empty dictionary `ctx = -1`. The
count reads only bytes before `idx`, so the decoder computes the same rank. This is ExCom's rule
and Buyanovsky's Lemma 3.

**3.2 The candidate window.** The candidates are the ranks `r` with `first <= r <= last`, where
`first = max(0, ctx - D + 1)` and `last = min(size - 1, ctx + D)`. The distance of a candidate is
`ctx - r`, so it lies in the range `-D .. D-1` that a `distanceBits`-bit signed field holds.

**3.3 The match.** A candidate at rank `r` is the entry at position `q`. Its match length is the
number of consecutive `k = 0, 1, ...` with `s[idx+k] == s[q+k]`, at most `L`, stopping at the end
of the segment. The comparison may run past `idx`, into the bytes being coded: a match may be longer
than the distance to its content. This is the thesis's §1.3.2 (overlapping buffers); the decoder
copies from `q` and, on reaching its own output, keeps repeating what it copied, which gives the
same bytes.

**3.4 The best candidate.** The longest match wins. Candidates are examined by growing
`|ctx - r|`, the rank above the context (distance `-k`) before the one below it (distance `+k`),
so among equal lengths the nearest wins; the scan stops at length `L`. ExCom walks the same way.
No candidate with a match of at least one byte means *no match*: the step is a literal.

**3.5 Deviations.**
- The thesis (§1.2, Example 1.2.1) takes the greater index of the two neighbours as the context
  reference, where 3.1 takes the one that agrees longer.
- The distance is `ctx - r`; the thesis writes `r - ctx`.

## 4. Segments and their ends

**4.1** A step never crosses the end of a segment. Every segment is stored as a block that records
the number of bytes it decodes to, and the decoder decodes until it has produced that many.

**4.2** A coder whose triplet ends in a literal (`simple`, `salomon2`, `valach`) needs a byte
left for it. A match that reaches the last byte of the segment therefore gives up its last byte:
the length is reduced by one, and that byte becomes the literal. A match of length 1 reaching the
end has nothing left to copy and becomes a literal. The thesis does not say what happens here.

**4.3** Every block is coded on its own: the dictionary and the entropy models start empty in each,
so no block depends on another. The models could carry over instead, but then a block could not be
stored raw without the decoder learning from bytes it never decodes, and blocks could not be
decoded in parallel. Starting the models afresh costs 0.03% on Calgary at 1 MB segments (990,715
bytes against 990,403 with a shared model), 0.07% at 300 KB and 0.27% at 100 KB.

**4.4** A block whose coded bytes are not fewer than its segment's own is stored as the segment
itself. Random input therefore grows only by the few bytes of the container, where it grew by
10.8% when every segment had to be coded.

## 5. The coders

A triplet's fields are written in the order listed. `dist` is the stored distance: two's
complement in `distanceBits` bits, sign-extended by the decoder. `len` has `lengthBits` bits. The
literal has 8 bits and `flag` 1 bit. After a step, `idx` moves by the number of bytes it coded,
and the dictionary gains those positions (2.1).

### 5.1 `simple`: `(dist, len, literal)`

Source: thesis §1.2 (Algorithm 1 and 2), with §1.3.2.

Every step is a triplet. With a match, `dist = ctx - r` and `len` is its length (after 4.2), and
the literal is the byte after it; the step codes `len + 1` bytes. Without a match, `dist = 0`,
`len = 0` and the literal is the byte at `idx`; a match that 4.2 shortened to nothing is such a
literal too.

Deviations: all of 2.3 and 3.5.

### 5.2 `salomon`: `(0, literal)` or `(1, dist, len)`

Source: thesis §1.3.1, from Salomon.

*No match:* `flag = 0` and the literal; 1 byte. *Match:* `flag = 1`, `dist`, `len`; `len` bytes,
and no literal. Because no literal follows, 4.2 does not apply and a match may end the segment.
The decoder tells the two apart by the flag.

Deviations: all of 2.3 and 3.5. The thesis is inconsistent about the field order: §1.3.1 gives
`(1, d, l)`, §3.2.1 and Table 5.8 give `(1, l, d)`. The code writes `dist` before `len`.

### 5.3 `salomon2`: `(0, literal)` or `(1, dist, len, literal)`

Source: thesis §3.2.1 ("the improvement to the triplet by Salomon", called Salomon2 in Table 5.8),
which is the form ExCom uses: the flagged form, with the literal kept on matches.

*No match* (or a match that 4.2 shortened to nothing): as `salomon`. *Match:* `flag = 1`, `dist`,
`len` (after 4.2), the literal; `len + 1`
bytes.

Deviations: all of 2.3 and 3.5. The thesis and ExCom order the fields `(1, l, d, c)`; the code
writes `dist` before `len`.

### 5.4 `valach`: `(len, literal)` or `(len, dist, literal)`

Source: thesis §3.2.1, after Valach's thesis, which found this form and `salomon2` the best in
compression ratio; ExCom's default coder.

The length comes first, so that the distance can be left out of a literal: `len = 0` is followed
by the literal alone, 1 byte. `len > 0` is followed by
`dist` and the literal; `len + 1` bytes.

Deviations: all of 2.3 and 3.5. ExCom also takes the better-agreeing context neighbour,
which 3.5 lists.

### 5.5 `lcp`: `(dist, len - lcp, literal)`

Source: thesis §3.3.1.

Laid out as `simple`, with a different rule for the best content. The **best** content is the
lexicographically smallest one among those with the maximal match length `M`, where a match is
measured up to `4L` bytes (an implementation limit). The **second** content is one that sorts below
the best. `lcp` is the length of the common prefix of the best and the second content, and the
triplet sends `M' - lcp` instead of the match length, which is never zero for a match and lets a
match exceed `L`: `M' = min(M, lcp + L)`, so the field is capped at `L` after subtracting, not before.

Both sides must be able to compute `lcp` from what the decoder has. So contents are compared, and
`lcp` is measured, only over the bytes before the position being coded, as unsigned bytes; a
content cut short there sorts before every longer content it is a prefix of. The decoder knows only
the best content, so `lcp` is defined from it: the largest common prefix with the best content
among all the candidates of the window that sort below it (none: 0). The encoder computes the same
number, and does so for the best content whatever the text goes on to say, which the thesis's
"second best by match length" would not allow. The decoder works `lcp` out only for a match, when
`len > 0`. The step codes `M' + 1` bytes; 4.2 applies to `M'`, and a match that 4.2 would leave with
no more than `lcp` bytes is written as a literal.

Why `lcp < M`: a candidate below the best that shared `M` bytes with it would match the text for
`M` bytes too, and so would have been chosen over the best.

Deviations: all of 2.3 and 3.5, except that the nearest match no longer wins ties. Over Calgary the
sizes are close to `simple`'s (`d = 6`: 994,471 against 991,407 at `l = 7`, 1,014,180 against
1,016,907 at `l = 4`; `d = 10`, `l = 6`: 961,928 against 962,098), because the smallest content is
often further from the context than the nearest one, and it costs about half the speed.

## 6. Worked examples

The current output for the thesis's two examples, with `distanceBits = lengthBits = 8`. The thesis
prints `(cnt - ctx, len, literal)`; the table converts it to this project's `ctx - cnt` for
comparison. The thesis's step 5 is printed `(1, 3, p)`, which its own formula turns into
`(-1, 3, p)`; converted, `1`.

`mississippi`, `simple`:

| | Triplets |
|---|---|
| Thesis §1.2, converted | `(0,0,m) (0,0,i) (0,0,s) (1,1,i) (1,3,p) (2,1,i)` |
| Current | `(0,0,m) (0,0,i) (0,0,s) (1,1,i) (0,3,p) (1,1,i)` |

Steps 5 and 6 differ by the context rule (3.5): the thesis takes the greater index, the code the
neighbour that agrees longer. The same input under the other coders:

| Coder | Triplets |
|---|---|
| `salomon` | `(0,m) (0,i) (0,s) (1,1,1) (1,1,4) (0,p) (1,1,1) (1,1,1)` |
| `salomon2` | `(0,m) (0,i) (0,s) (1,1,1,i) (1,0,3,p) (1,1,1,i)` |
| `valach` | `(0,m) (0,i) (0,s) (1,1,i) (3,0,p) (1,1,i)` (written as `(len, dist, literal)`) |

`sssss`, `simple`: the thesis (§1.3.2) gives `(0,0,s) (0,3,s)`, and so does the code: the second
step uses the only entry, rank 0.

## 7. Buyanovsky's associative coder

The reference for the planned `-tc acb` coder. It is what Buyanovsky's 1994 code (`AC.C`, no
licence: read it, never copy it) and his paper "Research method of pseudostochastic systems"
describe. The thesis (§2.1) says his algorithm is unknown and that distances are not
arithmetic-coded; both are wrong.

- **Dictionary.** A sorted array of pointers to every position of the frame (1,024 to 262,144, or
  more), ordered by the unbounded right-to-left context, compared as machine words. Insertion is
  a `memmove` into the array (the paper's "simple list", O(n) per insert). Once full, a new
  pointer replaces whichever of its two neighbours shares less with it. After a long match (at
  least 3 * log2 of the frame fill) only the first position is inserted.
- **Funnel of analogies.** From the current context's slot, walk outward on both sides, in order
  of weight, over neighbours whose context agrees with the current one for more than
  `SB = log2(Kc * N / 500)` bits, the paper's "stochastic component" (`N` = contexts stored). The
  agreement in bits comes from an XOR and bit scan over 4-byte words, capped at 256 bytes; the
  width is capped at 2,047 candidates. A candidate's weight (`EVR`) grows with context agreement
  and falls with rank distance.
- **Position.** The candidate is arithmetic-coded with probability proportional to its weight,
  plus an escape symbol whose weight adapts from recent success (`Sucsess`, `Swch` in the code).
  The encoder picks, in weight
  order, the first candidate with the strictly longest match, so ties go to the most similar
  context.
- **Length.** Coded as `Max - Pr_L - 1`, where `Pr_L` is the longest match among candidates coded
  before the chosen one. The decoder recovers `Pr_L` as the longest common prefix of the chosen
  content with those candidates, which equals it exactly (a candidate that matched less than the
  best one diverges from the best one where it diverged from the text). Lengths already shared by later
  candidates get a boosted probability. Matches never overlap the current position, and
  comparisons read only decoded bytes.
- **Literal.** Coded only when the match ended on a mismatch (not at a boundary or at 256). Every
  byte that a candidate with an equal-length match would predict is excluded, because it would
  have made the match longer. After a match, the literal model mixes the order-0 table with the
  next bytes of the funnel candidates for the new context.
- **Statistics.** Frequency tables sorted by count, updated in sliding windows (2,048 literals,
  1,024 lengths) through a ring buffer, and one custom 32-bit arithmetic coder. The length table
  starts at `{45, 13, 10, 7, 5, 4}` for excess lengths 0 to 5. This project's length model starts
  flat instead: over Calgary a flat start beat that table and several shapes of it by about 0.01%.
- **The paper's own coding.** The code of a string is the candidate number, a "difference bit"
  (which side of it the current content sorts in content order) and an "extract" length. This is
  the LCP idea done right; `AC.C` implements the simpler `Pr_L` variant above. ACB 1.17 caps the
  funnel at 16, 55 or 100 candidates per side (FAST, NORMAL, MAX); ACB 2.00 improves the modelling
  further, and both are closed.
- **Symmetry.** `AC.C` at its default level (`Kc 2`) fails to decode some inputs, so its encoder
  and decoder disagree somewhere. Every model decision must be derived from state both sides
  share.

## 8. Entropy coding of fields

The fields of a block go through one range coder, in the order a layout writes them, and each
field has an adaptive model of its own: every symbol counts 32 more each time it is coded, and the
counts are halved when their total would pass a limit (`max(2^16, 256 * symbols)`, at most 2^20).
The length field starts from the given `lengthFrequencies` and every other field flat. The models
start empty in every block (4.3). The thesis and its sources fix none of this.

`BIT_ARRAY` writes the fields at their full width instead, most significant bit first. It exists to
measure what the range coder gains.

`CONTEXT_ARITHMETIC` is a **variant**, not something a coder defines: a literal is coded against
the model of the byte before it, and never as the byte the chosen content continues with when the
match ended on a mismatch. A model for a byte is made when the byte is first seen, from the model of
all literals with every count divided by 16 (8, 64 and 256 were all worse). The excluded byte is
left out only when both sides can be sure the match ended on a mismatch: the length sent is below
the longest a triplet carries, the match is below the longest one measured (`4L` for `lcp`), and
the literal is not the last byte of the segment. Over Calgary (`valach`, `d = 6`, `l = 7`) it takes
the literals from 408,864 to 368,471 bytes and the file from 974,670 to 934,267 (4.1%), and costs
10% of the compression speed and 17% of the decompression speed.
