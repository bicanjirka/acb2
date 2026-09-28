package cz.cvut.fit.acb.utils;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author jiri.bican
 */
public final class TripletUtils {
	
	private TripletUtils() {
	}
	
	/** A triplet's fields in coding order, as {@code (a, b, c)}; bytes print signed. */
	public static String tripletString(int... fields) {
		return Arrays.stream(fields).mapToObj(String::valueOf).collect(Collectors.joining(", ", "(", ")"));
	}
}
