# ACB — Associative Coder of Buyanovsky

Master-thesis compressor (CTU FIT): LZ77-style context/content dictionary producing triplets,
then entropy-coded. Java 26, Maven, opened in IntelliJ from `pom.xml`.
Runtime deps: commons-cli, Log4j 2 (config in `src/main/resources/log4j2.xml`; tests use the
quieter `log4j2-test.xml`). Tests: JUnit 4.

```bash
mvn -q compile
mvn test                                        # runs only *Test classes - see Tests
mvn test -Dtest='*Test*'                        # every test, including the ACB round trips
mvn package && java -jar target/acb.jar input output [options]
```

`target/acb.jar` is shaded (dependencies bundled); the entry point is
`cz.cvut.fit.acb.ACBClient`.

## Working here

- Known gaps go in `TODO.md` (with **Where** and **Approach**), never an inline TODO. Closing a
  gap deletes its entry in the same commit.
- `CLAUDE.md` holds constraints only - no history, no feature narrative. A change that makes a
  line here false fixes it in the same commit.
- Multi-phase plan: commit after each phase, each phase green on its own.
- Commit subject lines are imperative mood, capitalized, no trailing period (`Add X`).
- Comments only for a non-obvious *why*; don't restate the code.
- IDE metadata (`.idea/`, `*.iml`, Eclipse files) is never committed; the pom is the project model.

## Packages (`cz.cvut.fit.acb.*`)

`ACB` (compress/decompress driver) · `ACBClient` (CLI) · `ACBFileIO` · `ACBProvider*` (wires
the strategies below from `ACBProviderParameters`) · `dictionary` (+ `core` order-statistic
trees) · `triplets` (+ `coder`: Simple, Salomon, Valach, LCP) · `coding` (triplet↔byte:
adaptive arithmetic, bit array) · `utils` (`ChainBuilder` pipeline). `nayuki.arithcode` is
vendored MIT code (Project Nayuki) - keep its licence notice, don't restyle it.

## Boundaries

- A compressed stream is only decodable with the same `ACBProviderParameters` it was encoded
  with; any change to triplet layout or coding breaks existing compressed files.
- Every triplet coder must round-trip at any segment size (`ACBTest.testSegmentedExecution`).
- `ACB` and `ACBFileIO.SaveParsedSingleton` hold state across calls; don't share one instance
  between independent streams.

## Tests

- Surefire runs only `*Test` classes, so `ACBTestCoder`, `ACBTestTripletCoder` and
  `ACBTestDictionaryStructures` are skipped by plain `mvn test`. They write to
  `src/test/resources/out/`, which must exist.
- Test inputs are read from `src/test/resources/in/` by relative path; run from the project root.

## Code style for new and rewritten code

- Values are immutable records with named factories; absence is `none()`/`Optional`, never
  `null`. Values that combine get an operation, identity and absorber.
- Constructor injection into `final` fields; small classes, interfaces of 1-5 methods. Inherit
  only for a closed set of variants, with `final` leaves.
- Branch on type with a visitor or a `switch` over a sealed type, never `instanceof`.
- Names put the role noun last (`ValachTripletCoder`). `this.` on every field access.
- Catch `Exception`, never `Throwable`; log or rethrow, never swallow or `printStackTrace`.
- Tests: behaviour-sentence method names, arrange/act/assert separated by blank lines,
  hand-written fakes, no Mockito.
