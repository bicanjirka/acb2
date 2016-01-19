package cz.cvut.fit.acb.triplets;

import java.io.InputStream;
import java.io.OutputStream;

public class TripletFactory {

	public static TripletWriter getWriter(String type, OutputStream out, int maxDistance, int maxLength) {

		if (type == null) {
			return null;
		}
		if (type.equalsIgnoreCase("default")) {
			return new DefaultTripletWriter(log2(maxDistance * 2), log2(maxLength), out);
		}

		return null;
	}

	public static TripletReader getReader(String type, InputStream in, int maxDistance, int maxLength) {

		if (type == null) {
			return null;
		}
		if (type.equalsIgnoreCase("default")) {
			return new DefaultTripletReader(log2(maxDistance * 2), log2(maxLength), in);
		}

		return null;
	}

	private static int log2(int value) {
		return Integer.SIZE - Integer.numberOfLeadingZeros(value);
	}
}
