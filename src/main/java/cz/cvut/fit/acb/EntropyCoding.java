package cz.cvut.fit.acb;

import cz.cvut.fit.acb.coding.BitArrayReader;
import cz.cvut.fit.acb.coding.BitArrayWriter;
import cz.cvut.fit.acb.coding.RangeTripletReader;
import cz.cvut.fit.acb.coding.RangeTripletWriter;
import cz.cvut.fit.acb.coding.TripletWriter;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.triplets.FieldSource;

import java.util.Arrays;
import java.util.Optional;

/**
 * How triplet fields become bytes. Like {@link TripletCoding}, a constant carries its format code
 * and the makers of its writer and reader.
 */
public enum EntropyCoding {

    ADAPTIVE_ARITHMETIC(0, RangeTripletWriter::new, RangeTripletReader::new),
    BIT_ARRAY(1, frequencies -> new BitArrayWriter(), (block, frequencies) -> new BitArrayReader(block));

    /** Makes the writer of one block; only length fields use the starting frequencies. */
    @FunctionalInterface
    private interface WriterFactory {
        TripletWriter create(int[] lengthFrequencies);
    }

    @FunctionalInterface
    private interface ReaderFactory {
        FieldSource create(byte[] block, int[] lengthFrequencies) throws MalformedStreamException;
    }

    private final int formatCode;
    private final WriterFactory writers;
    private final ReaderFactory readers;

    EntropyCoding(int formatCode, WriterFactory writers, ReaderFactory readers) {
        this.formatCode = formatCode;
        this.writers = writers;
        this.readers = readers;
    }

    /** The code the container format stores for this coding. */
    public int formatCode() {
        return this.formatCode;
    }

    public static Optional<EntropyCoding> byFormatCode(int code) {
        return Arrays.stream(values()).filter(coding -> coding.formatCode == code).findFirst();
    }

    public TripletWriter writer(int[] lengthFrequencies) {
        return this.writers.create(lengthFrequencies);
    }

    /** @throws MalformedStreamException if the block is too short to start reading */
    public FieldSource reader(byte[] block, int[] lengthFrequencies) throws MalformedStreamException {
        return this.readers.create(block, lengthFrequencies);
    }
}
