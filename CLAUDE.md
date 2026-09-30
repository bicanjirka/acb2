# ACB — Associative Coder of Buyanovsky

Master-thesis compressor (CTU FIT): LZ77-style context/content dictionary producing triplets,
then entropy-coded. Java 25, Maven, opened in IntelliJ from `pom.xml`.
Runtime deps: commons-cli, Log4j 2 (config in `src/main/resources/log4j2.xml`; tests use the
quieter `log4j2-test.xml`). Tests: JUnit 5 (Jupiter 6) + AssertJ + jqwik.

```bash
mvn -q compile
mvn test
mvn verify                                      # quick check: + Spotless, Checkstyle, coverage (target/site/jacoco)
mvn verify -Pfull                               # full check: also the tests tagged slow
mvn spotless:apply                              # fix formatting and import order
mvn test -Dtest=RoundTripTest#someSentenceName
mvn package && java -jar target/acb.jar input output [options]
java -jar target/acb.jar input.acb output -de   # settings come from the file
java -cp "target/acb.jar;target/test-classes" cz.cvut.fit.acb.harness.RatioHarness DIR   # or PerformanceHarness
```

`target/acb.jar` is shaded (dependencies bundled); the entry point is
`cz.cvut.fit.acb.ACBClient`.

## Working here

- Known gaps go in `TODO.md` (with **Where** and **Approach**), never an inline TODO. Closing a
  gap deletes its entry in the same commit.
- `CLAUDE.md` holds constraints only - no history, no feature narrative. A change that makes a
  line here false fixes it in the same commit.
- Multi-phase plan: commit after each phase, each phase green on its own. `mvn verify` before every
  commit; `mvn verify -Pfull` and both harnesses before a phase is closed.
- Commit subject lines are imperative mood, capitalized, no trailing period (`Add X`).
- Comments only for a non-obvious *why*; don't restate the code.
- IDE metadata (`.idea/`, `*.iml`, Eclipse files) is never committed; the pom is the project model.

## Packages (`cz.cvut.fit.acb.*`)

`Compressor` (the in-memory core, with the `OrderedMapper` that codes segments on an executor it is
given) · `ACBClient` (CLI, with `CliParser` and `CliRequest`) and `ACBFileIO` with `SegmentReader`,
`SegmentWriter` and `CompressedWriter` (its file side) · `ACBProvider` and
`ConfiguredACBProvider` (builds a segment's dictionaries, field writer and reader from a
`CompressionSettings` record) · a coder is one `TripletCoding` constant, which carries a `Coder`: a
`LayoutCoder` (layout, parser and `MatchRule`; its `SegmentCoding` runs the `triplets` pipeline) or the
`AssociativeCoder` (`-tc acb`; its `SegmentCoding` runs `associative`) · `dictionary` (+ the
`ContextIndex` behind it, in an encoder and a decoder view; a `MatchRule` constant carries its
matcher; the `AnalogyDictionary` and its funnels of analogies for `acb`) · `triplets` (the `Triplet`,
the segment encoder and decoder that share one update rule; `coder`: the layouts and parsers) ·
`associative` (Buyanovsky's coder: one step rule, `AssociativeSteps`, that the encoder and the decoder
both run, and the models behind it) · `coding` (field↔byte: range coder with adaptive models, bit
array, `CumulativeTable`) · `counts` (the Fenwick tree) · `format` (the on-disk container: header,
blocks, CRC32; `ContainerWriter` and `ContainerReader` do it a block at a time).

## Boundaries

- A compressed file carries its coding settings in a `format.StreamHeader`; decoding uses only
  those plus a free choice of dictionary structure. Any change to the container layout, the coder
  codes, how a coder lays out triplets, or what a coder's models code bumps `ContainerFormat.VERSION`.
- Every block decodes on its own: the dictionary and the entropy models start empty in each.
- Untrusted input is read only through `ContainerReader` (`ContainerFormat.decode` is its in-memory
  form), which verifies the checksum before trusting any of the stream and bounds every count before
  allocating. Never deserialize with `ObjectInputStream`.
- Every settings combination must round-trip at any segment size (`RoundTripTest`) and for any
  input (`RoundTripPropertiesTest`).
- `Compressor` holds no stream state and touches no files or console; `ACBClient` and
  `ACBFileIO` only adapt it. Coding logic goes in the core, never in the CLI. The core starts no
  threads: segments run on the executor of the `OrderedMapper` it is given, so everything a segment
  is coded with is made per segment or is immutable.

## Tests

- Quick tests must stay cheap: round trips over `SettingsCombination.all()` on small inputs. Large
  inputs and anything long-running or memory-hungry go in a class or method tagged
  `@Tag("slow")`, which only `-Pfull` runs.
- Round trips run in memory through `Compressor`, over every `fixtures.CorpusFile` (classpath
  `in/`). `fixtures.InterceptingProvider` wraps its
  components to snapshot dictionaries and check every triplet field of a layout coder. Files only in
  `@TempDir`.
- A combination with an open `TODO.md` defect is skipped in the grids with its reason, never
  passed; fixing it deletes the exclusion.
- `RatioRegressionTest` pins compressed sizes: a change that improves the ratio lowers the pins in
  the same commit. `harness.PerformanceHarness` fails under `PerformanceBudget`: raise the floors
  when a change makes the coders faster.
- Seed every `Random`. jqwik prints its seed on failure; keep generated inputs small.

## Code style for new and rewritten code

- Values are immutable records with named factories; absence is `none()`/`Optional`, never
  `null`. Values that combine get an operation, identity and absorber.
- Constructor injection into `final` fields; small classes, interfaces of 1-5 methods. Inherit
  only for a closed set of variants, with `final` leaves; otherwise compose. A class past about
  150 lines holds two ideas, and a nested public class is a file of its own.
- Branch on type with a visitor or a `switch` over a sealed type, never `instanceof`. A variation
  point is an enum constant or a strategy that carries its collaborators (`TripletCoding`,
  `MatchRule`), never a `switch` on it inside the operation.
- Names put the role noun last (`ValachTripletCoder`), a strategy is the prefix
  (`ConfiguredACBProvider`). Packages are by feature, never `utils` or `impl`. `this.` on every
  field access.
- Where a harness run shows a cost, the hot paths (range coder, frequency model, context index,
  dictionary search) keep primitive arrays, in-place mutation and lazily filled slots, and
  `ChunkedContextIndex` stays past the size limit; the class Javadoc says what was measured. Run both
  harnesses before and after touching them, and keep the mutation inside the leaf class.
- Catch `Exception`, never `Throwable`; log or rethrow, never swallow or `printStackTrace`.
- Tests: behaviour-sentence method names, arrange/act/assert separated by blank lines,
  hand-written fakes, no Mockito.
