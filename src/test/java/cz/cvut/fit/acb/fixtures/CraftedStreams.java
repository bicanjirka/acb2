package cz.cvut.fit.acb.fixtures;

import cz.cvut.fit.acb.CompressionSettings;
import cz.cvut.fit.acb.ConfiguredACBProvider;
import cz.cvut.fit.acb.coding.TripletWriter;
import cz.cvut.fit.acb.format.Block;
import cz.cvut.fit.acb.format.CompressedStream;
import cz.cvut.fit.acb.format.StreamHeader;
import cz.cvut.fit.acb.triplets.Triplet;
import cz.cvut.fit.acb.triplets.TripletLayout;

import java.util.List;
import java.util.function.Consumer;

/** Well-formed streams whose one block holds whatever fields a test writes, sound or not. */
public final class CraftedStreams {

    private CraftedStreams() {
    }

    /** A block of {@code rawLength} bytes that holds exactly {@code triplets}, laid out as the settings say. */
    public static CompressedStream oneBlock(CompressionSettings settings, int rawLength, Triplet... triplets) {
        TripletLayout layout = settings.tripletCoding().layoutCoder().orElseThrow()
                .layout(settings.distanceBits(), settings.lengthBits());
        return oneBlock(settings, rawLength, writer -> {
            for (Triplet triplet : triplets) {
                layout.write(triplet, writer);
            }
        });
    }

    /** A block of {@code rawLength} bytes that holds exactly the fields {@code fields} writes. */
    public static CompressedStream oneBlock(CompressionSettings settings, int rawLength,
                                            Consumer<TripletWriter> fields) {
        TripletWriter writer = new ConfiguredACBProvider(settings).writer();
        fields.accept(writer);
        return new CompressedStream(StreamHeader.of(settings), List.of(Block.coded(rawLength, writer.finish())));
    }
}
