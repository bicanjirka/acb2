package cz.cvut.fit.acb.utils;

import org.apache.logging.log4j.util.Supplier;

/**
 * @author jiri.bican
 */
public class TripletUtils {
	
	public static Supplier[] tripletString(int var1, int var2, byte var3) {
		Supplier supplier;
		supplier = () -> String.format("(%d, %d, %d)", var1, var2, var3);
		return new Supplier[]{supplier};
	}
	
	public static Supplier[] tripletString(int var1, int var2, int var3) {
		Supplier supplier;
		supplier = () -> String.format("(%d, %d, %d)", var1, var2, var3);
		return new Supplier[]{supplier};
	}
	
	public static Supplier[] tripletString(int var1, int var2) {
		Supplier supplier;
		supplier = () -> String.format("(%d, %d)", var1, var2);
		return new Supplier[]{supplier};
	}
	
	public static Supplier[] tripletString(int var1, byte var2) {
		Supplier supplier;
		supplier = () -> String.format("(%d, %d)", var1, var2);
		return new Supplier[]{supplier};
	}
	
	public static Supplier[] tripletString(int var1, int var2, int var3, byte var4) {
		Supplier supplier;
		supplier = () -> String.format("(%d, %d, %d, %d)", var1, var2, var3, var4);
		return new Supplier[]{supplier};
	}
}
