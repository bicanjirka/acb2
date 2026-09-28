package cz.cvut.fit.acb.triplets;

/**
 * @author jiri.bican
 */
public class TripletFieldId {
	private final int index;
	private final int bitSize;
	private final boolean isLength;
	
	public TripletFieldId(int index, int bitSize, boolean isLength) {
		this.index = index;
		this.bitSize = bitSize;
		this.isLength = isLength;
	}
	
	public TripletFieldId(int index, int bitSize) {
		this(index, bitSize, false);
	}
	
	public boolean isLength() {
		return isLength;
	}
	
	public int getIndex() {
		return index;
	}
	
	public int getBitSize() {
		return bitSize;
	}
	
	@Override
	public String toString() {
		return "[" + index + ", " + bitSize + ']';
	}
}
