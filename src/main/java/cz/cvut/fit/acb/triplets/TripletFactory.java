package cz.cvut.fit.acb.triplets;

import java.io.InputStream;
import java.io.OutputStream;

public class TripletFactory {

	public static TripletWriter getWriter(String type, OutputStream out, int maxDistance, int maxLength) {

		if (type == null) {
			return null;
		}
		if ("default".equalsIgnoreCase(type)) {
			return new DefaultTripletWriter(log2(maxDistance * 2), log2(maxLength), out);
		}
		if ("salomon".equalsIgnoreCase(type)) {
			return new SalomonTripletWriter(log2(maxDistance * 2), log2(maxLength), out);
		}
		if ("salomon+".equalsIgnoreCase(type)) {
			return new Salomon2TripletWriter(log2(maxDistance * 2), log2(maxLength), out);
		}
		if ("valach".equalsIgnoreCase(type)) {
			return new ValachTripletWriter(log2(maxDistance * 2), log2(maxLength), out);
		}

		return null;
	}

	public static TripletReader getReader(String type, InputStream in, int maxDistance, int maxLength) {

		if (type == null) {
			return null;
		}
		if ("default".equalsIgnoreCase(type)) {
			return new DefaultTripletReader(log2(maxDistance * 2), log2(maxLength), in);
		}
		if ("salomon".equalsIgnoreCase(type)) {
			return new SalomonTripletReader(log2(maxDistance * 2), log2(maxLength), in);
		}
		if ("salomon+".equalsIgnoreCase(type)) {
			return new Salomon2TripletReader(log2(maxDistance * 2), log2(maxLength), in);
		}
		if ("valach".equalsIgnoreCase(type)) {
			return new ValachTripletReader(log2(maxDistance * 2), log2(maxLength), in);
		}

		return null;
	}

	private static int log2(int value) {
		return Integer.SIZE - Integer.numberOfLeadingZeros(value);
	}
}
