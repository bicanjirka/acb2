package cz.cvut.fit.acb.coding;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

//---------------------------
//Class BitStreamInputStream
//--------------------------
//Implements an enhanced InputStream which allows you to read a
//stream of bit fields ranging in size from 1 bit (a true bit
//stream) to 32 bits (a stream of integers). The size of the current
//bitfield can be changed at any point while reading the stream.
//(c) Laurence Vanhelsuwe 1996. E-Mail: LVA@telework.demon.co.uk
//------------------------------------------------------------------

public class BitStreamInputStream extends FilterInputStream {
	
	final static int EIGHT = 8; // 8 bits per byte
	
	protected short buffer; // our BYTE bitstream read-ahead buffer declared as short to cope with EOF
	protected int bitsInCache; // how many unread bits left in our byte

	// ------------------------------------------------------------------
	public BitStreamInputStream(InputStream in) {
		super(in);
		bitsInCache = 0; // we haven't got any cached bits
	}

	// ------------------------------------------------------------------
	// Read a bitfield from the input stream. The number of bits read is
	// the current bitfield length. Bitfield can be on arbitrary bit boundaries.
	// ------------------------------------------------------------------
	public int read(int fieldSize) throws IOException {
		if (fieldSize > 32 || fieldSize < 1)
			throw new IllegalArgumentException("BitField size (" + fieldSize + ") no good. Has to be between 1 and 32.");

		int bitField; // what we're going to return to caller
		int bitsToRead; // remaining bits to assemble into BF
		int availableNumberOfBits;
		int OR_position;
		int rightAlignedBFPartial;
		bitField = 0; // start with empty jigsaw
		bitsToRead = fieldSize;
		OR_position = fieldSize;

		while (bitsToRead > 0) {
			if (bitsInCache == 0) {
				if ((buffer = (short) in.read()) == -1) {
					return -1; // reached EOF
				}
				bitsInCache = EIGHT; // we've got a full byte again
			}
			availableNumberOfBits = Math.min(bitsToRead, bitsInCache);
			rightAlignedBFPartial = buffer >> (EIGHT - availableNumberOfBits);
			// always keep next partial left aligned and clean
			buffer <<= availableNumberOfBits;
			buffer &= 255;
			OR_position -= availableNumberOfBits;
			// add bitfield subfield
			bitField |= rightAlignedBFPartial << OR_position;
			// track # of cached bits
			// track how much left to do

			bitsInCache -= availableNumberOfBits;
			bitsToRead -= availableNumberOfBits;
		}
		return bitField;
	}

	// ---------------------------------------------
	// The remaining methods are methods we override
	// from our parent class: FilterInputStream
	// ---------------------------------------------
	// ------------------------------------------------------------------
	// Overridden read() still reads a byte, but on any bit boundary.
	// ------------------------------------------------------------------
	public int read() throws IOException {
		return read(EIGHT);
	}

	// ------------------------------------------------------------------
	// Override block read() methods to use basic read() as building block.
	// The implementation we want for this read() is the same as that
	// for class InputStream.
	// According to the Java language spec, two elegant (and short
	// solution should work:
	// ((InputStream) this).read(..) // i.e. a cast
	// InputStream.read(..) // i.e. fully specifiying
	// Unfortunately neither work, so I am forced to paste in the
	// original code for InputStream.read(byte b[], int off, int len).
	// ------------------------------------------------------------------

	public int read(byte b[], int off, int len) throws IOException {

		if (len <= 0) {
			return 0;
		}
		int c = read();
		if (c == -1) {
			return -1;
		}
		b[off] = (byte) c;

		int i = 1;
		try {
			for (; i < len; i++) {
				c = read();
				if (c == -1) {
					break;
				}
				if (b != null) {
					b[off + i] = (byte) c;
				}
			}
		} catch (IOException ee) {
		}
		return i;
	}

	// ------------------------------------------------------------------
	// Overridden FilterInputStream.read(byte b[])
	// ------------------------------------------------------------------
	public int read(byte b[]) throws IOException {
		return read(b, 0, b.length);
	}

	// ------------------------------------------------------------------
	// Overridden FilterInputStream.skip(long n)
	// If any client relies heavily on skipping multi-byte strings in
	// the bitstream, then this method has to be re-implemented to be more
	// efficient. Current implementation is functional but highly inefficient.
	// ------------------------------------------------------------------
	public long skip(long n) throws IOException {
		long i;

		for (i = 0; i < n; i++) {
			if (read() == -1)
				break;
		}
		return i;
	}
}