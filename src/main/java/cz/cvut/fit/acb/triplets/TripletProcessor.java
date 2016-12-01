package cz.cvut.fit.acb.triplets;

/**
 * @author jiri.bican
 */
public interface TripletProcessor {
	int read(TripletFieldId fieldId);
	
	void write(TripletFieldId fieldId, int value);
	
	int getSize();
	
	void setSize(int size);
}
