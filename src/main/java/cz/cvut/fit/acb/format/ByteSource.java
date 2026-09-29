package cz.cvut.fit.acb.format;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Bytes that can be read from the start more than once, which {@link ContainerReader} needs: it
 * verifies the whole stream before it trusts any of it, then reads it again block by block.
 */
public interface ByteSource {

    long size() throws IOException;

    /** A new stream at the first byte; the caller closes it. */
    InputStream open() throws IOException;

    static ByteSource of(byte[] bytes) {
        return new ByteSource() {
            @Override
            public long size() {
                return bytes.length;
            }

            @Override
            public InputStream open() {
                return new ByteArrayInputStream(bytes);
            }
        };
    }
}
