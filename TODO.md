# Known gaps and future work

The single place for outstanding design gaps and planned work; the source carries no inline
`TODO` notes. Closing an item deletes its entry in the same commit.

## Known defects

Found by `RoundTripTest` and `RoundTripPropertiesTest`. The affected combinations are skipped
with a reason (`SettingsCombination.knownRoundTripDefect`/`knownDictionaryDefect`) or kept out of
the generated inputs, never silently passed; closing an item removes its exclusion.

### LCP dictionary diverges between encoder and decoder

The decoder's dictionary differs from the encoder's after a few updates, and segmented
decoding calls `select` with a negative rank. `ACBClient` refuses `-tc lcp` until this is
closed, because the files it wrote did not decompress. What is known:

- `DictionaryLCP.searchContent` mixes ranks and text positions: `bestIdx` holds a rank, but the
  equal-length branch assigns it `cnt` (a position) and compares `bestIdx + bestLen` as a
  position.
- The scheme sends `length - lcp` and lets the decoder recover `lcp` as the longest common
  prefix of the chosen content with its best neighbour. That only agrees with the encoder's
  second-best match against the text when there is no tie: a content matching the text as far as
  the best one shares at least `bestLen` with it, so the decoder adds back too much.

- **Where:** `dictionary.DictionaryLCP`, `triplets.coder.LCPTripletCoder`, the `-tc` check in
  `ACBClient.parseCommandLine`.
- **Approach:** fix the rank/position confusion, then define the second-best content so both
  sides compute it from shared state (for example, the best-matching neighbour of the chosen
  content, ties broken by rank). Remove the CLI refusal and the test exclusion together.

## Architecture in the house style

The core follows the style in `CLAUDE.md`; what is left here is an option, not a defect.

### Triplets as records

A coder emits each triplet as a `TripletSupplier` lambda that writes its fields straight into
the per-field entropy coders, and decodes one field at a time as the flags dictate. Not a
defect - an option, recorded with its cost.

- **Where:** `triplets.TripletSupplier`, every `encodeStep` in `triplets.coder`, `utils.TripletUtils`.
- **Approach:** a record per triplet shape (`Literal`, `Match`, `MatchWithLiteral`) with a
  `writeTo(TripletProcessor)` method would give triplets names, `toString` for tracing and
  equality for tests, replacing `TripletUtils`.
- **Why not yet:** it helps only the encoder (decoding still reads field by field), adds an object
  per triplet in the hot loop, and duplicates the layout each coder already states once. Worth it
  if tests or tooling start needing whole triplets, for example a triplet-level diff of two coders.

## Enforced style and cleanup

### Spotless and Checkstyle in `mvn verify`

Nothing checks formatting or the `CLAUDE.md` rules mechanically.

- **Where:** `pom.xml`, new `checkstyle.xml` and `checkstyle-imports.xml`.
- **Approach:** Spotless (import order matching IntelliJ, spaces, no unused imports) and
  Checkstyle with `ImportControl` (the core may not import `commons-cli` or `java.io` file
  access), `IllegalCatch`, and `RegexpSinglelineJava` bans on `printStackTrace` and
  `System.out` outside the CLI. `nayuki.arithcode` is excluded. The tabs-to-spaces reformat is
  its own commit, listed in `.git-blame-ignore-revs`.

### Dead code

- **Where:** `coding.RangeCoding`, `coding.ArithmeticCoding`.
- **Approach:** delete them; neither has a caller.

### Project documentation

`Readme-arith-coding.markdown` is the vendored Nayuki readme; the project has no README of its
own and no rationale document.

- **Where:** repository root, `docs/`.
- **Approach:** a `README.md` (what ACB is, build, CLI usage, the coder and dictionary
  options) and `docs/ARCHITECTURE.md` for the why behind the design. Move the Nayuki readme
  beside its package.

## Measurement and performance

### Compression-ratio benchmark on a standard corpus

The thesis-relevant number - ratio per triplet coder, dictionary structure and entropy coder -
has no reproducible harness.

- **Where:** new `cz.cvut.fit.acb.RatioHarness` (test scope or a separate source set).
- **Approach:** run every settings combination over a standard corpus (Canterbury), report
  ratio and throughput as a table, and verify each round trip while measuring.

### Microbenchmarks and a performance budget

`DictionaryBase.searchContent` calls `ost.select(i)` for every candidate instead of walking
neighbours, and `update` inserts keys one at a time; nothing measures either.

- **Where:** `dictionary.DictionaryBase`, `dictionary.DictionaryLCP`, `dictionary.core`.
- **Approach:** JMH benchmarks for dictionary search/update and each coder; a
  `PerformanceHarness` with an explicit budget that exits non-zero when over, run after
  changing per-byte code. Then optimise the neighbour walk and batched insert.

### Parallel segment compression

Each segment already gets its own dictionary, so segments are independent, but they are
compressed sequentially.

- **Where:** `Compressor.compress`, `ACBFileIO.SegmentReader`.
- **Approach:** compress segments on a bounded executor and write them in order; decompression
  stays sequential per stream but can decode segments in parallel once the container format
  records segment boundaries.
