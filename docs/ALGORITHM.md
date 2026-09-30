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
empty dictionary. The `acb` coder inserts fewer after a long step (7.8).

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

**4.5** Segments have a fixed size; the dictionary does not live on across them. Coding the whole
of Calgary (3,141,622 bytes as one input, `valach`, `d = 6`, `l = 7`) as one segment takes
985,709 bytes, against 988,224 at 1 MB segments (0.25% more) and 1,011,758 at 300 KB (2.6%). A
dictionary lifetime that adapts, as ExCom's does, could therefore win back at most 0.25% at the
default size, and would make blocks depend on each other, which the block format and parallel
compression rule out. ExCom's own policy is not an option to copy either: it decides from the
encoder's and the decoder's differing counts of bits, so the two sides can clear at different points.

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

## 7. Buyanovsky's associative coder: `acb`

Source: Buyanovsky's 1994 code (`AC.C`, which carries no licence: read, never copied) and his paper
"Research method of pseudostochastic systems". The thesis (§2.1) says his algorithm is unknown and
that distances are not arithmetic-coded; both are wrong. What follows is what this project's coder
does, rule by rule, and in 7.9 where that differs from `AC.C`.

The coder has no layout of fields: it codes the position, the length and the literal of a step against
distributions that the funnel of analogies of the step gives, and it does not use the entropy codings of
section 8 (`acb` is always `ADAPTIVE_ARITHMETIC`). `-d` sets how many entries a funnel takes from either
side of a context, `R = 2^(distanceBits - 1)`; `-l` the longest match, `L = 2^lengthBits - 1`; `-cd` how
many bytes of context are compared, `C`. The coder starts from `d = 6`, `l = 8`, `C = 255`.

**7.1 The dictionary.** As in 2.2, with `C` bytes of context, except that entries are added by 7.8.
Every entry is a position below `idx`, so its content has at least one byte known.

**7.2 The funnel of analogies.** For the position `idx` about to be coded, with `N` entries and at least 4
bytes before `idx` (otherwise the funnel is empty):

- `S = floor(log2(17 * N / 500))` is the number of bits by which a context must agree to be more than
  chance (`17` is `Kc` of `AC.C` at its best level, `Kc 0`; `S` is 0 below 59 entries).
- The **agreement** of an entry is how many bits its context has in common with that of `idx`, going back
  from each, the nearest byte first and the top bit of a byte first, over at most `C` bytes and never
  past the start of the segment.
- The funnel takes entries outward from the place where `idx` would sort, at most `R` on each side. The
  weight of the entry `k` places from that place, `k >= 1`, with agreement `a` and `b = floor(a / 8)` whole
  bytes, is 0 if `a - S < 2`, and otherwise
  `(b + floor(1024 / k) + ((a + 1) mod 8)) * floor(log2(1 + b))`. A side ends at its first entry of
  weight 0. The two sides are merged by weight: the next entry is the one above the place if it weighs at
  least as much as the next one below, else the one below. The funnel is that order, with the weights.
- The weights grow with the agreement and fall with the distance in ranks, which the paper asks of them
  and does not fix; the formula is the shape that `AC.C` uses.

Sorted as they are, the agreement only falls outward, and the agreement of an entry is the least of that
of the entry before it and the bytes the two have in common. So the dictionary keeps, next to each entry,
how much it shares with the one before it, and the first bytes of its context, and the walk compares
bytes only for the two entries next to `idx`. This implements the rule above and changes nothing in it.

**7.3 The step.** The encoder finds for every candidate `j` of the funnel the match `T(j)`: how many bytes
`s[idx ..]` has in common with the content of `j`, at most `L` and never more than `idx - j`, so that a
match does not reach the bytes being coded. The best candidate is the first in funnel order with the
longest match, if that is at least 1. A step codes, in this order:

1. **The position**: a symbol from 0 to the funnel's size, against a distribution in which candidate `i`
   (symbol `i + 1`) weighs its weight (7.4) and symbol 0, the **escape**, means that no candidate matches.
   An empty funnel codes no position. After an escape, or with an empty funnel, go to 4.
2. **The length** `M` of the match (7.5).
3. **The literal** after the match, the byte that ended it, if the match ended on a mismatch (7.6).
   Then the step is over: it coded `M + 1` bytes, or `M` if no literal follows.
4. **The literal** of an escape or of an empty funnel, the byte at `idx`: the step codes 1 byte.

What a step leaves in the dictionary is 7.8.

**7.4 The position distribution.** The weights of the funnel are shifted right, none to less than 1, until
they total at most `2^19`. The escape weighs `T * (e + 1) / (n - e + 1)`, where `T` is the total of the
shifted weights and `e` and `n` count the recent steps of the same *class* that ended in an escape and all
of them. The class is `min(15, floor(log2(w)))` for the weight `w` of the first candidate, and both
counts are halved when `n` reaches 256. The escape weighs at least 1 and at most `2^19`. The paper asks
for an escape that adapts to recent success and fixes no rule.

**7.5 The length.** Let `c` be the chosen candidate. `P` is the longest common prefix of the content of `c`
with the content of any candidate before `c` in the funnel, compared over the bytes before `idx`
(0 if `c` is the first). The match `M` is more than `P`, since `c` is the first candidate to reach it: a
candidate before `c` matched fewer bytes, the text agrees with `c` on those bytes and differs from the
candidate where it does, and so the two contents share exactly what that candidate matched. The decoder
therefore finds `P` itself, and `M - P - 1` is what is coded: a number from 0 to
`min(L, idx - c) - P - 1`, against an adaptive model of the recent such numbers (the frequency model of
section 8, starting from `lengthFrequencies`, flat by default) restricted to the numbers that are possible.
Candidates after `c` that share `t > P` bytes with `c`, `t` at most the longest possible match, make the
number `t - P - 1` likelier, since a text that leaves `c` where they do has a match of `t`: if `m`
candidates share `t`, its frequency gains `total * (n + floor(log2(n + 2))) / 10`, with `n = m - floor(m / 4)`.

**7.6 When a literal follows.** None follows a match of `L` bytes, which may have been cut short, or one that
reaches the end of the segment. Otherwise the **excluded** bytes are `s[e + M]` for every candidate `e`
whose content shares `M` bytes with that of `c` (`c` among them) and for which `e + M < idx`, so that the
byte is known. Each of those candidates matched as long as `c` and went on with that byte, which would have
made the match longer than the longest, so the text cannot go on with it. If no byte is excluded, `c` reaches
`idx`, nothing says that the match ended on a mismatch, and no literal follows. An escape excludes the first
byte of every candidate, for the same reason.

**7.7 The literal distribution.** The frequency of a byte is that of an adaptive order-0 model over the
literals of the segment (section 8's model, flat at the start), with every excluded byte at 0. After a match,
the funnel of the context that the match made, at `idx + M`, votes. It is a second funnel, with the weights
`a * floor(log2 a)` if `a - S >= 4` and 0 otherwise, and no distance. Each candidate gives its weight to the
byte at its content. A byte that is excluded gets no vote; every other byte has its frequency raised by
`(d + 1) * total * v / V`, where `total` is the model's total over the bytes that are left, `v` the weight
of the votes for the byte, `V` all the votes and `d = floor(log2(floor(v / 2048)))`, or 0 if `v < 4096`.
The literal after an escape, or of an empty funnel, has no votes: the candidates that would vote are all
excluded.

**7.8 What a step leaves in the dictionary.** The positions it coded, `idx .. idx + n - 1`, except that a step
of at least `3 * floor(log2 N)` bytes adds only `idx`, since the rest is a copy of what the dictionary
holds. `AC.C` does the same. Entries are therefore not always every position below `idx`, as 2.1 says of
the other coders. Over Calgary (`d = 6`) adding every position gave 845,743 bytes, 0.3% more than the
rule's 843,240, and was 6% slower; a limit of 12 bytes instead of `3 * floor(log2 N)` gave 841,798.

**7.9 Deviations from `AC.C`.** All of these are choices where the source is silent or ways of implementing
what it says; none changes a field, a context, a candidate or a length.

- A segment is a frame (section 4) and its dictionary keeps every position: there is no replacement of the
  neighbour that shares less once the frame is full. A frame of 256 KB gave 849,813 bytes on Calgary and
  1 MB gave 837,107, so keeping everything is the stronger; the segment size caps what is kept.
- The agreement is over `C <= 255` bytes (`AC.C`: 256), and near the start of a segment over the bytes that
  there are, where `AC.C` counts 0 when fewer than 4 lie before.
- `R` is a setting. `AC.C` takes up to 1,024 candidates from each side; ACB 1.17 has presets of 16, 55 and
  100. Over Calgary (`l = 8`, `C = 255`) `d = 4, 5, 6, 7, 8, 9` give 878,363, 855,407, 843,240, 837,618,
  835,696 and 834,710 bytes. `AC.C` gave 846,912 at 55 per side, 839,769 at 128 and 837,107 at its widest.
- The escape is a miss rate per class of funnel (7.4); `AC.C` adapts two counters (`Sucsess`, `Swch`) from
  the ratio of hits and misses. Counting by the size of the funnel gave 843,934 bytes, by nothing 846,395.
- The statistics are the halving frequency models of section 8, which with increments of 32 forget as a
  sliding window of 2,048 symbols does; `AC.C` keeps rings of the last 2,048 literals and 1,024 lengths. Its
  length table starts at `{45, 13, 10, 7, 5, 4}`; this one starts flat.
- The weight constants are `AC.C`'s. A nearness of 256 over the distance instead of 1,024 gave 0.13% less,
  which wider funnels do not share, so the constant is kept.
- The longest match is `2^l - 1`, not 256, and a match measured at `L` bytes carries no literal, as at 256
  in `AC.C`.
- The votes of the second funnel come from all its entries; `AC.C` leaves out the entry just below the
  context, which is a slip, not a rule.
- The coder of section 8 codes the symbols, with totals of at most `2^20`, where `AC.C` has a 32-bit coder of
  its own.
- What a step does is written once (`AssociativeSteps`), and the encoder and the decoder differ only in
  where a symbol comes from, so no decision can be made from what one side has and the other has not.
  `AC.C` at its default level (`Kc 2`) fails to decode book1.

**7.10 The paper's own coding.** The paper codes a string by the number of the candidate that agrees most, a
"difference bit" (on which side of it the string sorts in content order) and an "extract" length. That is
the idea of the `lcp` coder done right, where `AC.C` implements the simpler coding above. Neither the
paper's coding nor ACB 2.00's modelling is implemented (`TODO.md`).

## 8. Entropy coding of fields

The fields of a block go through one range coder, in the order a layout writes them, and each
field has an adaptive model of its own: every symbol counts 32 more each time it is coded, and the
counts are halved when their total would pass a limit (`max(2^16, 256 * symbols)`, at most 2^20).
The length field starts from the given `lengthFrequencies` and every other field flat. The models
start empty in every block (4.3). The thesis and its sources fix none of this.

`BIT_ARRAY` writes the fields at their full width instead, most significant bit first. It exists to
measure what the range coder gains.

`CONTEXT_ARITHMETIC` is a **variant**, not something a coder defines: a literal is coded against
the model of the byte before it, and never as a byte that would have made the match longer. A model
for a byte is made when the byte is first seen, from the model of all literals with every count
divided by 16 (8, 64 and 256 were all worse). The bytes left out are those that every candidate of the
window (3.2) that matched as far as the chosen one would have gone on with, the chosen content among
them (a candidate whose next byte lies at or past `idx` is left out, since that byte is not known yet);
after no match at all, the first byte of every candidate of the window. They are left out only when both
sides can be sure: for
a match, the length sent is below the longest a triplet carries, the match is below the longest one
measured (`4L` for `lcp`), and the literal is not the last byte of the segment; for no match, the
literal is at least the longest one measured (`4L`) from the end of the segment, since a match cut back
to nothing there leaves a literal that is a byte the candidates start with. The decoder finds the set
from the contents of the candidates and the content it has copied, so nothing is sent.

Over Calgary (`valach`, `d = 6`, `l = 7`) it takes the literals from 408,864 to 351,149 bytes and the file
from 974,670 to 916,945 (5.9%), and costs 29% of the compression speed and 39% of the decompression
speed. Leaving out only the one byte the chosen content goes on with, as an earlier version did, gave
368,471 literal bytes and a file of 934,267, for 10% and 17% of the speed: the rest of the set is worth
1.9% of the file for about 20% of the speed.
