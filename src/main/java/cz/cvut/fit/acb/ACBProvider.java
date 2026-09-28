package cz.cvut.fit.acb;

import java.util.List;

import cz.cvut.fit.acb.coding.TripletWriter;
import cz.cvut.fit.acb.dictionary.ByteSequence;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.triplets.TripletProcessor;
import cz.cvut.fit.acb.triplets.coder.TripletCoder;

/**
 * Builds the components one stream is coded with. {@link Compressor} asks for fresh ones per
 * stream and segment, so implementations hold no stream state.
 *
 * @author jiri.bican
 */
public interface ACBProvider {
	Dictionary getDictionary(ByteSequence sequence);
	
	TripletCoder getCoder(ByteSequence sequence, Dictionary dictionary);
	
	TripletWriter getTripletWriter();
	
	TripletProcessor getTripletReader(List<byte[]> payload);

}
