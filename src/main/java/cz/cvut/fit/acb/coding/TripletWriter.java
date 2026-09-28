package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.triplets.TripletProcessor;

import java.util.List;

/** Takes one stream's triplet fields and turns them into payload arrays; used once per stream. */
public interface TripletWriter extends TripletProcessor {

    /** The size announcement first, then one array per field index. */
    List<byte[]> finish();
}
