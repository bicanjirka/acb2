package cz.cvut.fit.acb.utils;

import cz.cvut.fit.acb.triplets.TripletSupplier;
import org.apache.logging.log4j.util.Supplier;

public class TripletUtils {

	public static String printSalomon(TripletSupplier t) {
		if (t == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
//		sb.append('(');
//		if (data[0] == 0 && data[1] == 0) {
//			sb.append(0).append(", ").append(data[2]).append(')');
//		} else {
//			sb.append(1).append(", ").append(data[0]).append(", ").append(data[1]).append(')');
//		}
		return sb.toString();
	}

	public static String printSalomon2(TripletSupplier t) {
		if (t == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
//		sb.append('(');
//		if (data[0] == 0 && data[1] == 0) {
//			sb.append(0).append(", ").append(data[2]).append(')');
//		} else {
//			sb.append(1).append(", ").append(data[0]).append(", ").append(data[1]).append(", ").append(data[2]).append(')');
//		}
		return sb.toString();
	}

	public static String printValach(TripletSupplier t) {
		if (t == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
//		sb.append('(');
//		if (data[1] == 0) {
//			sb.append(0).append(", ").append(data[2]).append(')');
//		} else {
//			sb.append(data[1]).append(", ").append(data[0]).append(", ").append(data[2]).append(')');
//		}
		return sb.toString();
	}

	public static Supplier[] tripletString(int var1, int var2, byte var3) {
		Supplier supplier;
		supplier = () -> String.format("(%d, %d, %d)", var1, var2, var3);
		return new Supplier[] {supplier};
	}
	
	public static Supplier[] tripletString(int var1, int var2, int var3) {
		Supplier supplier;
		supplier = () -> String.format("(%d, %d, %d)", var1, var2, var3);
		return new Supplier[] {supplier};
	}
	
	public static Supplier[] tripletString(int var1, int var2) {
		Supplier supplier;
		supplier = () -> String.format("(%d, %d)", var1, var2);
		return new Supplier[] {supplier};
	}
	
	public static Supplier[] tripletString(int var1, byte var2) {
		Supplier supplier;
		supplier = () -> String.format("(%d, %d)", var1, var2);
		return new Supplier[] {supplier};
	}
	
	public static Supplier[] tripletString(int var1, int var2, int var3, byte var4) {
		Supplier supplier;
		supplier = () -> String.format("(%d, %d, %d, %d)", var1, var2, var3, var4);
		return new Supplier[] {supplier};
	}
}
