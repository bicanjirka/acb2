package cz.cvut.fit.acb.format;

import java.util.List;

/** A header and the coded payload arrays, in the order the triplet-to-byte converter produced them. */
public record CompressedStream(StreamHeader header, List<byte[]> payload) {

    public CompressedStream {
        payload = List.copyOf(payload);
    }
}
