package cz.cvut.fit.acb.harness;

import cz.cvut.fit.acb.TripletCoding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HarnessTest {

    @TempDir
    Path corpus;

    @Test
    void theRatioHarnessReportsOneRowPerCombinationOverEveryFileOfTheCorpus() throws IOException {
        Files.writeString(this.corpus.resolve("a"), "mississippi mississippi");
        Files.writeString(this.corpus.resolve("b"), "swiss miss swiss miss");
        ByteArrayOutputStream captured = new ByteArrayOutputStream();

        List<RatioHarness.Row> rows = RatioHarness.run(this.corpus, new String[]{"coders=simple,valach", "l=4,7"},
                new PrintStream(captured, true, StandardCharsets.UTF_8));

        assertThat(rows).extracting(RatioHarness.Row::label).containsExactly(
                "simple d=6 l=4 c=10 arith", "simple d=6 l=7 c=10 arith", "valach d=6 l=4 c=10 arith", "valach d=6 l=7 c=10 arith");
        assertThat(rows).allSatisfy(row -> assertThat(row.inputBytes()).isEqualTo(44));
        assertThat(captured.toString(StandardCharsets.UTF_8)).contains("2 files, 44 bytes", "valach d=6 l=7 c=10 arith");
    }

    @Test
    void theRatioHarnessRejectsAnOptionItDoesNotKnow() throws IOException {
        Files.writeString(this.corpus.resolve("a"), "mississippi");

        assertThatThrownBy(() -> RatioHarness.run(this.corpus, new String[]{"speed=fast"},
                new PrintStream(new ByteArrayOutputStream()))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void thePerformanceHarnessPassesWhenNoCoderHasAFloor() {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();

        boolean withinBudget = PerformanceHarness.run(2_048, 1, PerformanceBudget.none(),
                new PrintStream(captured, true, StandardCharsets.UTF_8));

        assertThat(withinBudget).isTrue();
        assertThat(captured.toString(StandardCharsets.UTF_8)).contains("valach").doesNotContain("UNDER BUDGET");
    }

    @Test
    void thePerformanceHarnessFailsWhenACoderIsUnderItsFloor() {
        PerformanceBudget impossible = new PerformanceBudget(Map.of(
                TripletCoding.VALACH, new PerformanceBudget.Floor(1e9, 1e9)));
        ByteArrayOutputStream captured = new ByteArrayOutputStream();

        boolean withinBudget = PerformanceHarness.run(2_048, 1, impossible,
                new PrintStream(captured, true, StandardCharsets.UTF_8));

        assertThat(withinBudget).isFalse();
        assertThat(captured.toString(StandardCharsets.UTF_8)).contains("UNDER BUDGET");
    }
}
