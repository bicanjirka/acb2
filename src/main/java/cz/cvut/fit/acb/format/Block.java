package cz.cvut.fit.acb.format;

import java.util.Arrays;

/**
 * One segment as the container stores it: the segment itself when coding did not shrink it, or the
 * segment coded. Bytes are copied in and out, so a block never changes.
 */
public sealed interface Block {

    /** Bytes of the segment this block decodes to. */
    int rawLength();

    /** A copy of what the container stores for the block. */
    byte[] bytes();

    /** How many bytes the block takes in the container. */
    int storedLength();

    static Block stored(byte[] segment) {
        return new Stored(segment);
    }

    static Block coded(int rawLength, byte[] coded) {
        return new Coded(rawLength, coded);
    }

    /** A segment kept as it is. */
    record Stored(byte[] bytes) implements Block {

        public Stored {
            if (bytes.length < 1) {
                throw new IllegalArgumentException("a block holds at least one byte");
            }
            bytes = bytes.clone();
        }

        @Override
        public int rawLength() {
            return this.bytes.length;
        }

        @Override
        public int storedLength() {
            return this.bytes.length;
        }

        @Override
        public byte[] bytes() {
            return this.bytes.clone();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Stored that && Arrays.equals(this.bytes, that.bytes);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(this.bytes);
        }

        @Override
        public String toString() {
            return "Stored[" + this.bytes.length + " bytes]";
        }
    }

    /** A segment of {@code rawLength} bytes, coded with the stream's settings. */
    record Coded(int rawLength, byte[] bytes) implements Block {

        public Coded {
            if (rawLength < 1) {
                throw new IllegalArgumentException("a block holds at least one byte: " + rawLength);
            }
            bytes = bytes.clone();
        }

        @Override
        public int storedLength() {
            return this.bytes.length;
        }

        @Override
        public byte[] bytes() {
            return this.bytes.clone();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Coded that && this.rawLength == that.rawLength
                    && Arrays.equals(this.bytes, that.bytes);
        }

        @Override
        public int hashCode() {
            return 31 * this.rawLength + Arrays.hashCode(this.bytes);
        }

        @Override
        public String toString() {
            return "Coded[" + this.bytes.length + " bytes for " + this.rawLength + "]";
        }
    }
}
