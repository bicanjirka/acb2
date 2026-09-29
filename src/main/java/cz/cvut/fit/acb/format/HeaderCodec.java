package cz.cvut.fit.acb.format;

import cz.cvut.fit.acb.CompressionSettings;
import cz.cvut.fit.acb.EntropyCoding;
import cz.cvut.fit.acb.TripletCoding;
import cz.cvut.fit.acb.coding.LengthFrequencies;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * The header of the container: the settings of a stream, from {@code distanceBits} to the length
 * frequencies as {@link ContainerFormat} lays them out. Reading checks every value a stream can carry
 * wrongly before anything is allocated from it.
 */
final class HeaderCodec {

    private HeaderCodec() {
    }

    static void write(StreamHeader header, DataOutputStream out) throws IOException {
        out.writeByte(header.distanceBits());
        out.writeByte(header.lengthBits());
        out.writeByte(header.tripletCoding().formatCode());
        out.writeByte(header.entropyCoding().formatCode());
        out.writeByte(header.contextDepth());
        out.writeInt(header.segmentSize());
        int[] frequencies = header.lengthFrequencies().toArray();
        out.writeInt(frequencies.length);
        for (int frequency : frequencies) {
            out.writeInt(frequency);
        }
    }

    static StreamHeader read(WireReader in) throws MalformedStreamException {
        int distanceBits = bits(in.unsignedByte(), "distance");
        int lengthBits = bits(in.unsignedByte(), "length");
        int tripletCode = in.unsignedByte();
        int entropyCode = in.unsignedByte();
        TripletCoding tripletCoding = TripletCoding.byFormatCode(tripletCode)
                .orElseThrow(() -> new MalformedStreamException("Unknown triplet coding code " + tripletCode));
        EntropyCoding entropyCoding = EntropyCoding.byFormatCode(entropyCode)
                .orElseThrow(() -> new MalformedStreamException("Unknown entropy coding code " + entropyCode));
        int contextDepth = in.unsignedByte();
        if (contextDepth < 1) {
            throw new MalformedStreamException("Invalid context depth " + contextDepth);
        }
        int segmentSize = in.int32();
        if (segmentSize < 1) {
            throw new MalformedStreamException("Invalid segment size " + segmentSize);
        }
        int alphabet = CompressionSettings.lengthAlphabetSize(lengthBits);
        int frequencyCount = in.count(Integer.BYTES, "frequency");
        if (frequencyCount > alphabet) {
            throw new MalformedStreamException("Invalid frequency count " + frequencyCount);
        }
        int[] given = new int[frequencyCount];
        for (int i = 0; i < given.length; i++) {
            given[i] = in.int32();
        }
        LengthFrequencies frequencies;
        try {
            frequencies = LengthFrequencies.of(given);
            frequencies.requireFits(alphabet);
        } catch (IllegalArgumentException e) {
            throw new MalformedStreamException("Invalid length frequencies: " + e.getMessage());
        }
        return new StreamHeader(distanceBits, lengthBits, tripletCoding, entropyCoding, frequencies, segmentSize,
                contextDepth);
    }

    private static int bits(int bits, String what) throws MalformedStreamException {
        if (bits < 1 || bits > CompressionSettings.MAX_FIELD_BITS) {
            throw new MalformedStreamException("Invalid " + what + " bit width " + bits);
        }
        return bits;
    }
}
