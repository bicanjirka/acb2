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

**2.2 Order.** Entries are ordered by context, compared right to left over at most the last 10
bytes. Entries `p` and `q` compare `s[p-k]` with `s[q-k]` for `k = 1 .. min(p, q, 10)`; the first
difference decides, as **signed** bytes. If none differs, the smaller position sorts first. So a
context that runs out of bytes sorts before every longer one it is a prefix of, and among equal
contexts the latest position has the highest rank.

Everything in this order reads only bytes before the position, so the decoder rebuilds the same
dictionary from the bytes it has decoded.

**2.3 Deviations.**
- The depth of 10 bytes and the signed comparison are fixed constants, not part of the stream.
  The thesis compares the whole context, and every source assumes unsigned bytes.

## 3. Searching

**3.1 The current context.** For the position `idx` about to be coded, `ctx` is the rank of the
entry immediately below where `idx` would be inserted, the *predecessor*. With an empty
dictionary, or when `idx`'s context sorts below every entry, `ctx = -1`. The decoder computes the
same rank.

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
- The predecessor is always the context reference. The thesis (§1.2, Example 1.2.1) takes the
  greater index of the two neighbours; ExCom and Buyanovsky's Lemma 3 take whichever neighbour
  agrees with the current context longer.
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

The **best** content is the lexicographically smallest one among those with the maximal match
length `M`. The **second** content is one that sorts below the best. `lcp` is the length of the
common prefix of the best and the second content, and the triplet sends `M - lcp` instead of
`M`, which is never zero for `M > 0` and lets `M` exceed `L`.

Both sides must be able to compute `lcp` from what the decoder has. So contents are compared, and
`lcp` is measured, only over bytes before the position being coded; a content cut short there
sorts as smaller. The decoder knows only the best content: it takes the largest common prefix
with it among the contents that sort below it, and does this only when `len > 0`. The `len`
field is capped at `L` after subtracting, not before.

Status: **not implemented.** The first version picked the second content by comparing against
text the decoder does not have, so the two sides diverged; it was removed, and there is no
`-tc lcp` until `TODO.md` closes the gap.

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

Steps 5 and 6 differ by the context rule (3.5): the thesis takes the successor, the code the
predecessor. The same input under the other coders:

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
