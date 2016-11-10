package cz.cvut.fit.acb.triplets;

/**
 * @author jiri.bican
 */
public interface TripletSupplier {
	void visit(TripletProcessor visitor);
	
}