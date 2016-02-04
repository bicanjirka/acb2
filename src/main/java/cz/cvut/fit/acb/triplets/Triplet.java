package cz.cvut.fit.acb.triplets;

public class Triplet {

	private int distance;
	private int lenght;
	private byte symbol;

	public Triplet(int distance, int lenght, byte b) {
		this.distance = distance;
		this.lenght = lenght;
		this.symbol = b;
	}

	public Triplet(int distance, int lenght, int i) {
		this(distance, lenght, (byte) i);
	}

	public int getDistance() {
		return distance;
	}

	public int getLenght() {
		return lenght;
	}

	public byte getSymbol() {
		return symbol;
	}

	@Override
	public String toString() {
		return "(" + distance + ", " + lenght + ", " + symbol + ")";
	}

}