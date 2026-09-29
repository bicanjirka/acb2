package cz.cvut.fit.acb;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CliParserTest {

    private final CliParser parser = new CliParser();

    private CliRequest.Work work(String... args) throws CliParser.UsageException {
        return (CliRequest.Work) this.parser.parse(args);
    }

    @Test
    void twoPathsAloneMeanACompressionWithDefaults() throws CliParser.UsageException {
        CliRequest.Work work = this.work("in", "out");

        assertThat(work).isEqualTo(CliRequest.Work.of(Path.of("in"), Path.of("out")));
        assertThat(work.mode()).isEqualTo(CliRequest.Mode.COMPRESS);
        assertThat(work.settings()).isEqualTo(CompressionSettings.defaults());
    }

    @Test
    void everyOptionLandsInTheRequest() throws CliParser.UsageException {
        CliRequest.Work work = this.work("in", "out", "-de", "-f", "-d", "9", "-l", "3", "-tc", "valach",
                "-af", "5,4", "-cd", "16", "-ec", "context_arithmetic", "-log", "debug", "-m");

        assertThat(work.mode()).isEqualTo(CliRequest.Mode.DECOMPRESS);
        assertThat(work.force()).isTrue();
        assertThat(work.logLevel()).isEqualTo(Optional.of(Level.DEBUG));
        assertThat(work.measure()).isEqualTo(CliRequest.Measure.console());
        assertThat(work.settings()).isEqualTo(CompressionSettings.defaults()
                .withDistanceBits(9).withLengthBits(3).withEntropyCoding(EntropyCoding.CONTEXT_ARITHMETIC)
                .withTripletCoding(TripletCoding.VALACH)
                .withLengthFrequencies(5, 4).withContextDepth(16));
    }

    @Test
    void theThreadsOptionSetsHowManyThreadsCodeSegments() throws CliParser.UsageException {
        assertThat(this.work("in", "out", "-j", "3").threads()).isEqualTo(3);
        assertThat(this.work("in", "out").threads()).isEqualTo(CliRequest.Work.DEFAULT_THREADS);
    }

    @Test
    void fewerThanOneThreadIsAUsageError() {
        assertThatThrownBy(() -> this.work("in", "out", "-j", "0")).isInstanceOf(CliParser.UsageException.class);
        assertThatThrownBy(() -> this.work("in", "out", "-j", "many")).isInstanceOf(CliParser.UsageException.class);
    }

    @Test
    void theBitStreamOptionSelectsTheBitArray() throws CliParser.UsageException {
        assertThat(this.work("in", "out", "-bs").settings().entropyCoding()).isEqualTo(EntropyCoding.BIT_ARRAY);
    }


    @Test
    void measuringIntoAFileNamesTheFile() throws CliParser.UsageException {
        assertThat(this.work("in", "out", "-mreport.txt").measure())
                .isEqualTo(CliRequest.Measure.toFile(Path.of("report.txt")));
    }

    @Test
    void helpWinsOverMissingArguments() throws CliParser.UsageException {
        assertThat(this.parser.parse(new String[]{"-h"})).isEqualTo(new CliRequest.Help());
    }

    @Test
    void valuesNoStreamCanCarryAreUsageErrors() {
        assertThatThrownBy(() -> this.work("in", "out", "-d", "17")).isInstanceOf(CliParser.UsageException.class);
        assertThatThrownBy(() -> this.work("in", "out", "-l", "x")).isInstanceOf(CliParser.UsageException.class);
        assertThatThrownBy(() -> this.work("in", "out", "-cd", "256")).isInstanceOf(CliParser.UsageException.class);
        assertThatThrownBy(() -> this.work("in", "out", "-cd", "0")).isInstanceOf(CliParser.UsageException.class);
        assertThatThrownBy(() -> this.work("in", "out", "-ec", "nonsense")).isInstanceOf(CliParser.UsageException.class);
        assertThatThrownBy(() -> this.work("in", "out", "-af", "3,0")).isInstanceOf(CliParser.UsageException.class);
        assertThatThrownBy(() -> this.work("in", "out", "-tc", "nonsense"))
                .isInstanceOf(CliParser.UsageException.class);
        assertThatThrownBy(() -> this.work("in", "out", "-log", "loud")).isInstanceOf(CliParser.UsageException.class);
    }

    @Test
    void thePathsMustBeExactlyTwo() {
        assertThatThrownBy(() -> this.work()).isInstanceOf(CliParser.UsageException.class);
        assertThatThrownBy(() -> this.work("only")).isInstanceOf(CliParser.UsageException.class);
        assertThatThrownBy(() -> this.work("a", "b", "c")).isInstanceOf(CliParser.UsageException.class);
    }
}
