package cz.cvut.fit.acb.triplets;

/**
 * @author jiri.bican
 */
public interface TripletProcessor {
	void set(TripletFieldId fieldId, int value);
	
	int get(TripletFieldId fieldId);
}
