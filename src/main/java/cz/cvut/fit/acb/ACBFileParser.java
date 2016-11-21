/*
 * Copyright 2002-2016 Ataccama Software, s.r.o. All rights reserved.
 * ATACCAMA PROPRIETARY/CONFIDENTIAL.
 * Any use of this source code is prohibited without prior written
 * permission of Ataccama Software, s.r.o.; Czech Republic, Id.no.: 28235550
 * http://www.ataccama.com
 */
package cz.cvut.fit.acb;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jiri.bican
 */
public class ACBFileParser {
	private static final Logger logger = LogManager.getLogger();
	private final int bufferUnit;
	
	public ACBFileParser() {
		this(1_000_000);
	}
	
	public ACBFileParser(int bufferUnit) {
		this.bufferUnit = bufferUnit;
	}
	
	public void open(Path path, Consumer<ByteBuffer> byteBufferConsumer) {
		try {
			SeekableByteChannel sbc = Files.newByteChannel(path);
			logger.info("Opened file '{}' [size = {}]", path, sbc.size());
			ExecutorService service = getExecutorService();
			List<Callable<Object>> tasks = new ArrayList<>();
			
			while (sbc.position() < sbc.size()) {
				int size = (int) Math.min(bufferUnit, sbc.size() - sbc.position());
				ByteBuffer bb = ByteBuffer.allocate(size);
				sbc.read(bb);
				
				tasks.add(Executors.callable(() -> {
					logger.info("Starting {} with {} bytes.", Thread.currentThread().getName(), size);
					byteBufferConsumer.accept(bb);
				}));
			}
			service.invokeAll(tasks);
//			service.shutdown();
//			try {
//				service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private ExecutorService getExecutorService() {
		return Executors.newWorkStealingPool();
	}
	
	public void arrayObjectSave(List<byte[]> bytes, Path output) {
		try {
			Files.deleteIfExists(output);
			OutputStream os = Files.newOutputStream(output);
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(bytes);
			oos.close();
			
			int byteSize = bytes.stream().mapToInt(value -> value.length).sum();
			long finalSize = Files.size(output);
			logger.info("Compressed into '{}' [size = {}, bytes = {}]", output, finalSize, byteSize);
			StringBuilder sb = new StringBuilder("Outputted byte array:");
			for (int i = 0; i < bytes.size(); i++) {
				byte[] b = bytes.get(i);
				sb.append("\n").append(i).append(": ").append(b.length);
			}
			logger.info(sb.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
