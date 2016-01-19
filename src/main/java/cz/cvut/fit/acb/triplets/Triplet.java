package cz.cvut.fit.acb.triplets;

public class Triplet {

	private int distance;
	private int lenght;
	private char symbol;

	public Triplet(int distance, int lenght, char ch) {
		this.distance = distance;
		this.lenght = lenght;
		this.symbol = ch;
	}
	public int getDistance() {
		return distance;
	}
	public int getLenght() {
		return lenght;
	}

	public char getSymbol() {
		return symbol;
	}

	@Override
	public String toString() {
		return "(" + distance + ", " + lenght + ", " + symbol + ")";
	}
	
}