/*
 * Copyright 2002-2016 Ataccama Software, s.r.o. All rights reserved.
 * ATACCAMA PROPRIETARY/CONFIDENTIAL.
 * Any use of this source code is prohibited without prior written
 * permission of Ataccama Software, s.r.o.; Czech Republic, Id.no.: 28235550
 * http://www.ataccama.com
 */
package cz.cvut.fit.acb.utils;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author jiri.bican
 */
public class ACBTestValueStorage {
	private List<byte[]> bytes;
	private Queue<ByteBuffer> buffers;
	
	public void storeByteArray(List<byte[]> bytes) {
		this.bytes = bytes;
	}
	
	public List<byte[]> readByteArray() {
		return bytes;
	}
	
	public void storeByteBuffer(ByteBuffer byteBuffer, Consumer<ByteBuffer> byteBufferConsumer) {
		if (buffers == null) {
			buffers = new LinkedList<>();
		}
		buffers.add(byteBuffer);
		byteBufferConsumer.accept(byteBuffer);
	}
	
	public void testStoredByteBuffers(ByteBuffer byteBuffer) {
		if (byteBuffer == null) {
			assertNotNull(buffers);
			assertNull(buffers.poll());
			return;
		}
		assertNotNull(buffers);
		ByteBuffer poll = buffers.poll();
		assertNotNull(poll);
		assertArrayEquals(byteBuffer.array(), poll.array());
	}
	
	public void testStoredByteBuffers1(ByteBuffer byteBuffer, Consumer<ByteBuffer> byteBufferConsumer) {
		testStoredByteBuffers(byteBuffer);
		byteBufferConsumer.accept(byteBuffer);
	}
}
