# Known gaps and future work

The single place for outstanding design gaps and planned work; the source carries no inline
`TODO` notes. Closing an item deletes its entry in the same commit.

Suggested order: harden the decode boundary, replace the frequency table, build the ratio
harness, then the encoder/decoder symmetry refactor together with container format v2. The LCP
fix and parallel segments should mostly fall out of the last step.

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
  content, ties broken by rank). Remove the CLI refusal and the test exclusion together. Easier
  after the symmetry refactor below, where the update rule exists once.

## Decode boundary

### Header bit widths allow allocating gigabytes

The header accepts bit widths up to `CompressionSettings.MAX_FIELD_BITS` (30), and the adaptive
arithmetic model allocates `2^bits + 1` ints, plus as many again for the cumulative table.
Compressing with `-d 30` fails with `OutOfMemoryError` at a 512 MB heap. The decoder builds the
same model from the header, so a crafted file of about 30 bytes with a recomputed CRC should do
the same. This breaks the rule that decoding bounds every count before allocating. Wide fields
are not useful anyway: `-d 24` compressed a 3.5 KB text to 98% of its size.

- **Where:** `CompressionSettings.MAX_FIELD_BITS`, `format.ContainerFormat.bits`,
  `coding.AdaptiveArithmeticCompress`/`AdaptiveArithmeticDecompress`.
- **Approach:** cap the widths to what the model supports (around 16), in the settings and
  therefore in decoding. Add a test that a header with a wider field is rejected as malformed.

### Bad payloads fail with arbitrary exceptions or garbage

A well-formed container with a corrupt payload is not caught: an out-of-range rank gives an NPE
or `IndexOutOfBoundsException`, not `MalformedStreamException`. `AdaptiveArithmeticDecompress`
swallows an `IOException` and returns symbol 0, so decoding continues on invented data.
`ValachTripletCoder.decodeStep` casts a `-1` end-of-stream read straight to a byte. Tests corrupt
only the container, never the payload.

- **Where:** `Compressor.decompress`, every `decodeStep` in `triplets.coder`,
  `dictionary.DictionaryBase.copy`/`select`, `coding.AdaptiveArithmeticDecompress`.
- **Approach:** validate distances and ranks against the dictionary size in the decoder and
  throw `MalformedStreamException`; stop reading past a `-1`. Add a jqwik property that feeds
  random payloads inside a valid container through `Compressor.decompress` and accepts only
  success or `MalformedStreamException`.

### Swallowed I/O errors in the coding layer

`printStackTrace` followed by carrying on, against the error rule in `CLAUDE.md`. In the encoder
a swallowed failure produces a corrupt `.acb` without any error.

- **Where:** `coding.AdaptiveArithmeticCompress` (`compress`, `terminate`),
  `coding.AdaptiveArithmeticDecompress` (constructors, `decompress`), `coding.BitArrayComposer.compress`,
  `coding.BitArrayDecomposer.decompress`.
- **Approach:** these are in-memory streams, so rethrow as `UncheckedIOException` on the
  encoder side and as `MalformedStreamException` on the decoder side.

### Constants the decoder depends on are not part of the format

`Dictionary.ReverseIndexComparator.MAGIC_CONST = 10` sorts contexts by their last 10 bytes
only. That decides the ranks, so changing it silently breaks every existing file, yet it is
neither in the header nor tied to `ContainerFormat.VERSION`. The same holds for the policy that
the dictionary resets per segment while the entropy model lives for the whole stream.

- **Where:** `dictionary.Dictionary.ReverseIndexComparator`, `Compressor`, `format.ContainerFormat`.
- **Approach:** name them as format constants beside `VERSION` (or make the context depth a
  setting stored in the header), and measure what a longer context depth does to the ratio.

## Entropy coding

### Frequency table costs O(alphabet) per symbol and never rescales

`nayuki.arithcode.SimpleFrequencyTable.increment` drops the cumulative table, and the next
`getLow` rebuilds it, so every coded symbol costs O(alphabet) time and allocates a new array.
On a 3.5 KB text, `-d 16` took 169 ms against 31 ms at the defaults, and the ratio went from
0.58 to 0.74. Frequencies are never halved and one model per field lives for the whole stream,
so after about 2^30 symbols in a field Nayuki's `MAX_TOTAL` check throws and large inputs fail
to compress.

- **Where:** `coding.AdaptiveArithmeticCompress`, `coding.AdaptiveArithmeticDecompress`.
- **Approach:** our own `FrequencyTable` in `coding`: a Fenwick tree for cumulative counts with
  periodic halving, which also lets the model follow changes in the data. The vendored Nayuki
  code stays untouched. Changes coded output, so it bumps `VERSION`.

### The bit-array writer does not fit the per-field template

`BitArrayComposer.getArray` returns `null` for all fields but one, gated by a `doReturn` flag,
and `TripletToByteConverter.finish` has to skip the nulls. The bit-array writer is not per-field
at all.

- **Where:** `coding.BitArrayComposer`, `coding.TripletToByteConverter`.
- **Approach:** a separate implementation of the field sink (see the split below) that writes
  one interleaved bit stream, not a subclass of the per-field template.

## Encoder/decoder symmetry

### One update rule, shared by both sides

Each coder class is both encoder and decoder, and its mode depends on the runtime type of its
`ByteSequence`: every `decodeStep` downcasts to `ByteBuilder`. `encodeStep` and `decodeStep`
each restate the dictionary update and the end-of-segment shortening of a match (for example
`ValachTripletCoder` lines 41-53 against 85-90). That duplication is where the LCP defect lives.

This replaces the earlier "Triplets as records" option, which held that records help only the
encoder: their value is on the decoder side, because the update rule would then exist once.

- **Where:** `triplets.coder` (all coders), `triplets.TripletSupplier`, `utils.TripletUtils`,
  `dictionary.DictionaryInfo`.
- **Approach:** a sealed `Triplet` (`Literal`, `Match`, `MatchWithLiteral`); per coder a pure
  layout (triplet to fields and back) and an encoder-only parser (`DictionaryInfo` to
  `Triplet`); one `apply(Triplet, SegmentState)` that updates dictionary and position, used by
  both sides. Measure the allocation cost with the benchmarks rather than assume it.
  `DictionaryInfo` becomes a sealed `NoMatch | Match` record instead of a `-1` content.

### Split the two-way triplet processor

`TripletProcessor` has `read`, `write`, `getSize` and `setSize`; the writer throws on the read
half and the reader on the write half. The segment size travels through this field channel and
lands in `payload[0]`, which `Compressor.decompress` then has to validate.

- **Where:** `triplets.TripletProcessor`, `coding.TripletWriter`,
  `coding.TripletToByteConverter`, `coding.ByteToTripletConverter`, `Compressor`,
  `format.StreamHeader`.
- **Approach:** a field sink and a field source as separate interfaces; the segment size moves
  into `StreamHeader`.

### Split the dictionary by audience

`Dictionary` mixes encoder methods (`search`, `searchContent`), decoder methods (`copy`,
`select`) and test-only members (`clone`, and `equals`/`hashCode`, used only by
`fixtures.DictionarySnapshots`), and exposes rank arithmetic. Related faults:

- `ByteBuilder.clone` copies the whole capacity, so the clone's length is its capacity;
  `DictionaryBase.equals` compares a prefix to work around it.
- `ByteBuilder.equals` compares contents but `hashCode` hashes array identity.
- `ByteArray.array()` returns its internal array.
- `OrderStatisticTree` inherits a 16-method `Serializable` interface and uses five methods;
  keys are boxed `Integer`s on the hot path.

- **Where:** `dictionary.Dictionary`, `dictionary.DictionaryBase`, `dictionary.ByteBuilder`,
  `dictionary.ByteArray`, `dictionary.core`, `fixtures.DictionarySnapshots`.
- **Approach:** narrow encoder and decoder views of the dictionary; snapshots built by the test
  harness from public queries instead of `clone`/`equals` on production classes; fix
  `ByteBuilder` clone and equality; an order-statistic interface of the five methods used,
  ideally over `int` keys.

### Probable off-by-one in the candidate window

`DictionaryBase.searchContent` scans from `lo + 1`. When `lo` is clamped to 0, rank 0 is never
a candidate, although its distance fits the field. Costs ratio, not correctness.

- **Where:** `dictionary.DictionaryBase.searchContent`, `dictionary.DictionaryLCP.searchContent`.
- **Approach:** confirm with a test on a context whose rank is below `maxDistance`, then start
  at `lo` when clamped. Changes coded output, so it bumps `VERSION`.

## Container format v2

### Record the length; drop the end-of-stream sentinels

The end of the stream is signalled three ways: `-1` from field reads, `Integer.MAX_VALUE` from
`decodeStep`, and `TripletCoder.DecodeFlag`. Every arithmetic field stream also spends an EOF
symbol, which is why its alphabet is `2^n + 1`.

- **Where:** `format.ContainerFormat`, `format.StreamHeader`, `triplets.coder.BaseTripletCoder`,
  `Compressor.decompress`, `coding`.
- **Approach:** store the original length (or segment count and last segment size) in the
  header and let the decoder loop by count. The sentinels, `DecodeFlag` and the EOF symbols go
  away. Bumps `VERSION`.

### Per-segment blocks

Payload arrays are per field and span the whole file, the payload is held as one
`List<byte[]>`, and `ContainerFormat` reads the whole file. Segmenting bounds the dictionary but
not memory, and the entropy model shared across segments blocks parallel compression.

- **Where:** `format.ContainerFormat`, `Compressor`, `ACBFileIO`.
- **Approach:** one block per segment (raw length, then the byte length of each field), streamed
  in and out. Decide deliberately whether the entropy model resets per segment and measure what
  the reset costs in ratio. Bumps `VERSION`.

## Enforced style and cleanup

### Spotless and Checkstyle in `mvn verify`

Nothing checks formatting or the `CLAUDE.md` rules mechanically.

- **Where:** `pom.xml`, new `checkstyle.xml` and `checkstyle-imports.xml`.
- **Approach:** Spotless (import order matching IntelliJ, spaces, no unused imports) and
  Checkstyle with `ImportControl` (the core may not import `commons-cli` or `java.io` file
  access), `IllegalCatch`, and `RegexpSinglelineJava` bans on `printStackTrace`, inline `TODO`
  and `System.out` outside the CLI. `nayuki.arithcode` is excluded. The tabs-to-spaces reformat
  is its own commit, listed in `.git-blame-ignore-revs`.

### Dead code and leftovers

- **Where:** `coding.RangeCoding`, `coding.ArithmeticCoding` (no callers); inline `// TODO` in
  `DictionaryBase.searchContent`/`update` and `DictionaryLCP.searchContent`; commented-out code
  in `DictionaryBase` and `DictionaryLCP`; `Serializable` on `ByteSequence` and
  `BinarySearchTree`; `ByteBuilder.crop`.
- **Approach:** delete them; the inline TODOs are covered by the performance entry below.

### Coder choice lives in several switches

Choosing a triplet coding picks both the dictionary and the coder in two `switch`es in
`ACBProviderImpl`, and a third table in `ContainerFormat` maps it to a code. The exhaustive
switches do force every case to be handled, so this is low priority.

- **Where:** `ACBProviderImpl`, `TripletCoding`, `format.ContainerFormat`.
- **Approach:** a `CoderScheme` value carrying its dictionary choice and format code, so a new
  coder is added in one place.

### CLI parses into mutable fields

`ACBClient` fills mutable instance fields during parsing, keeps `Options` static, and
reconfigures Log4j as a side effect of parsing.

- **Where:** `ACBClient`.
- **Approach:** parse into an immutable `CliRequest` record (paths, mode, measure target,
  settings, log level) and apply the log level in `run`.

### Project documentation

The project has no rationale document, and the vendored Nayuki readme
(`Readme-arith-coding.markdown`) sits at the repository root instead of beside its package.

- **Where:** `docs/`, repository root.
- **Approach:** `docs/ARCHITECTURE.md` for the why behind the design. Move the Nayuki readme
  beside its package and update the link in `README.md`.

## Measurement and performance

### Compression-ratio benchmark on a standard corpus

The thesis-relevant number - ratio per triplet coder, dictionary structure and entropy coder -
has no reproducible harness, and no test guards it: a refactor that halves compression passes
every test. Build it before the structural changes above, so each one is measured.

- **Where:** new `cz.cvut.fit.acb.RatioHarness` (test scope or a separate source set), a
  ratio-regression test.
- **Approach:** run every settings combination over a standard corpus (Canterbury), report
  ratio and throughput as a table, and verify each round trip while measuring. Pin the current
  ratios per combination on the test corpus with a small tolerance, so a regression fails.

### Microbenchmarks and a performance budget

`DictionaryBase.searchContent` calls `ost.select(i)` for every candidate instead of walking
neighbours, and `update` inserts keys one at a time; nothing measures either. No test covers
large inputs.

- **Where:** `dictionary.DictionaryBase`, `dictionary.DictionaryLCP`, `dictionary.core`.
- **Approach:** JMH benchmarks for dictionary search/update and each coder; a
  `PerformanceHarness` with an explicit budget that exits non-zero when over, run after
  changing per-byte code and including one large input. Then optimise the neighbour walk and
  batched insert.

### Parallel segment compression

Each segment already gets its own dictionary, but segments are compressed sequentially, and the
entropy model shared across the stream ties them together.

- **Where:** `Compressor.compress`, `ACBFileIO.SegmentReader`.
- **Approach:** after per-segment blocks, compress segments on a bounded executor and write
  them in order; decode segments in parallel the same way.
