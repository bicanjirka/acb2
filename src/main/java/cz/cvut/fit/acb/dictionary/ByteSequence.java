package cz.cvut.fit.acb.dictionary;

public interface ByteSequence {

	byte[] array();
	byte[] array(int start, int end);
	byte byteAt(int index);
	int length();
}
