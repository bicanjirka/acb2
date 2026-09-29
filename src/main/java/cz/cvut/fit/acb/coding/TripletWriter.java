package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.triplets.FieldSink;

import java.util.List;

/** Takes the fields of one block and turns them into the bytes the block is stored as; used once. */
public interface TripletWriter extends FieldSink {

    /** Ends the block and returns its bytes; no field may be written afterwards. */
    byte[] finish();

    /** What each kind of field cost in the block; only meaningful after {@link #finish()}. */
    List<FieldCost> costs();
}
