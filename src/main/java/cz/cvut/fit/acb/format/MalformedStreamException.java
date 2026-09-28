package cz.cvut.fit.acb.format;

import java.io.IOException;
import java.io.Serial;

/** The bytes are not a complete, intact ACB stream this version can read. */
public final class MalformedStreamException extends IOException {

    @Serial
    private static final long serialVersionUID = 1L;

    public MalformedStreamException(String message) {
        super(message);
    }
}
