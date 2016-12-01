package cz.cvut.fit.acb.dictionary;

import java.io.Serializable;

/**
 * @author jiri.bican
 */
public interface ByteSequence extends Serializable {
	
	byte[] array();
	byte[] array(int start, int end);
	byte byteAt(int index);
	
	ByteSequence clone();
	int length();
}
