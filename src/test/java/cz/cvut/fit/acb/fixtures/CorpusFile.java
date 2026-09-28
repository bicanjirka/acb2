package cz.cvut.fit.acb.fixtures;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.stream.Stream;

/** A small test input from {@code src/test/resources/in}, loaded from the classpath. */
public record CorpusFile(String name, byte[] bytes) {

    private static final String[] NAMES = {"aaaa", "binary", "loremipsum", "mississippi", "swissmiss"};

    public static Stream<CorpusFile> all() {
        return Stream.of(NAMES).map(CorpusFile::load);
    }

    private static CorpusFile load(String name) {
        try (InputStream in = CorpusFile.class.getResourceAsStream("/in/" + name)) {
            if (in == null) {
                throw new IllegalStateException("Missing test resource /in/" + name);
            }
            return new CorpusFile(name, in.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String toString() {
        return this.name;
    }
}
