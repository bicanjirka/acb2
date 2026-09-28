package cz.cvut.fit.acb;

/**
 * @author Jiri.Bican
 */
public class ACBProviderParameters {
	
	public int distanceBits = 6;
	public int lengthBits = 4;
	public TripletCoderE tc = TripletCoderE.SIMPLE;
	public CoderE cd = CoderE.ADAPTIVE_ARITHMETIC;
	public OrderStatisticTreeE tr = OrderStatisticTreeE.RED_BLACK;
	
	public enum TripletCoderE {
		SALOMON, SALOMON2, SIMPLE, VALACH, LCP
	}
	
	public enum CoderE {
		ADAPTIVE_ARITHMETIC, BIT_ARRAY
	}
	
	public enum OrderStatisticTreeE {
		RED_BLACK, BST, ST
	}
}
