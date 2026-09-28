package cz.cvut.fit.acb;

import org.apache.logging.log4j.Level;

import java.nio.file.Path;
import java.util.Optional;

/** What the command line asked for: the help text, or a compression or decompression to carry out. */
public sealed interface CliRequest permits CliRequest.Help, CliRequest.Work {

    enum Mode {
        COMPRESS, DECOMPRESS
    }

    /** Where measurements of each file go. */
    sealed interface Measure permits Measure.Off, Measure.Console, Measure.ToFile {

        static Measure off() {
            return new Off();
        }

        static Measure console() {
            return new Console();
        }

        static Measure toFile(Path file) {
            return new ToFile(file);
        }

        record Off() implements Measure {
        }

        record Console() implements Measure {
        }

        record ToFile(Path file) implements Measure {
        }
    }

    record Help() implements CliRequest {
    }

    /**
     * A job for {@code input} into {@code output}. For a decompression only the dictionary structure of
     * the settings applies, since the rest comes from each file.
     */
    record Work(Path input, Path output, Mode mode, Measure measure, CompressionSettings settings,
                Optional<Level> logLevel, boolean force) implements CliRequest {

        public static Work of(Path input, Path output) {
            return new Work(input, output, Mode.COMPRESS, Measure.off(), CompressionSettings.defaults(),
                    Optional.empty(), false);
        }

        public Work withMode(Mode newMode) {
            return new Work(this.input, this.output, newMode, this.measure, this.settings, this.logLevel, this.force);
        }

        public Work withMeasure(Measure newMeasure) {
            return new Work(this.input, this.output, this.mode, newMeasure, this.settings, this.logLevel, this.force);
        }

        public Work withSettings(CompressionSettings newSettings) {
            return new Work(this.input, this.output, this.mode, this.measure, newSettings, this.logLevel, this.force);
        }

        public Work withLogLevel(Level level) {
            return new Work(this.input, this.output, this.mode, this.measure, this.settings, Optional.of(level),
                    this.force);
        }

        public Work withForce(boolean overwrite) {
            return new Work(this.input, this.output, this.mode, this.measure, this.settings, this.logLevel,
                    overwrite);
        }
    }
}
