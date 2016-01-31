package cz.cvut.fit.acb.utils;

import cz.cvut.fit.acb.triplets.Triplet;

public class TripletUtils {

	public static String printSalomon(Triplet t) {
		if (t == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		sb.append('(');
		if (t.getDistance() == 0 && t.getLenght() == 0) {
			sb.append(0).append(", ").append(t.getSymbol()).append(')');
		} else {
			sb.append(1).append(", ").append(t.getDistance()).append(", ").append(t.getLenght()).append(')');
		}
		return sb.toString();
	}

	public static String printSalomon2(Triplet t) {
		if (t == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		sb.append('(');
		if (t.getDistance() == 0 && t.getLenght() == 0) {
			sb.append(0).append(", ").append(t.getSymbol()).append(')');
		} else {
			sb.append(1).append(", ").append(t.getDistance()).append(", ").append(t.getLenght()).append(", ").append(t.getSymbol()).append(')');
		}
		return sb.toString();
	}

	public static String printValach(Triplet t) {
		if (t == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		sb.append('(');
		if (t.getLenght() == 0) {
			sb.append(0).append(", ").append(t.getSymbol()).append(')');
		} else {
			sb.append(t.getLenght()).append(", ").append(t.getDistance()).append(", ").append(t.getSymbol()).append(')');
		}
		return sb.toString();
	}
	
}
