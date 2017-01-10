/*
 * Copyright 2002-2016 Ataccama Software, s.r.o. All rights reserved.
 * ATACCAMA PROPRIETARY/CONFIDENTIAL.
 * Any use of this source code is prohibited without prior written
 * permission of Ataccama Software, s.r.o.; Czech Republic, Id.no.: 28235550
 * http://www.ataccama.com
 */
package cz.cvut.fit.acb;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
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
public class ACBFileIO {
	private static final Logger logger = LogManager.getLogger();
	private final int bufferUnit;
	
	public ACBFileIO() {
		this(1_000_000);
	}
	
	public ACBFileIO(int bufferUnit) {
		this.bufferUnit = bufferUnit;
	}
	
	public void openParallel(Path path, Consumer<ByteBuffer> byteBufferConsumer) {
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
			tasks.add(Executors.callable(() -> byteBufferConsumer.accept(null)));
			service.invokeAll(tasks);
//			service.shutdown();
//			try {
//				service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private ExecutorService getExecutorService() {
		return Executors.newWorkStealingPool();
	}
	
	public void openParse(Path path, Consumer<ByteBuffer> byteBufferConsumer) {
		try {
			SeekableByteChannel sbc = Files.newByteChannel(path);
			logger.debug("Opened file '{}' [size = {}] parsed into {} units, {} bytes each", path, sbc.size(), Math.ceil(sbc.size() / (double) bufferUnit), bufferUnit);
			
			while (sbc.position() < sbc.size()) {
				int size = (int) Math.min(bufferUnit, sbc.size() - sbc.position());
				ByteBuffer bb = ByteBuffer.allocate(size);
				sbc.read(bb);
				
				byteBufferConsumer.accept(bb);
			}
			byteBufferConsumer.accept(null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void saveObject(List<byte[]> bytes, Path output) {
		try {
			Files.deleteIfExists(output);
			OutputStream os = Files.newOutputStream(output);
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(bytes);
			oos.close();
			logAfterSave(bytes, output);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void openObject(Path path, Consumer<List<byte[]>> listConsumer) {
		try {
			InputStream is = Files.newInputStream(path);
			ObjectInputStream ois = new ObjectInputStream(is);
			List<byte[]> bytes = (List<byte[]>) ois.readObject();
			listConsumer.accept(bytes);
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private void logAfterSave(List<byte[]> bytes, Path output) throws IOException {
		int byteSize = bytes.stream().mapToInt(value -> value.length).sum();
		long finalSize = Files.size(output);
		logger.debug("Compressed into '{}' [size = {}, overhead = {}]", output, finalSize, finalSize - byteSize);
		StringBuilder sb = new StringBuilder("Outputted byte array:");
		for (int i = 0; i < bytes.size(); i++) {
			byte[] b = bytes.get(i);
			sb.append("\n").append(i).append(": ").append(b.length);
		}
		logger.debug(sb.toString());
	}
	
	public void saveArray(List<byte[]> bytes, Path output) {
		try {
			Files.deleteIfExists(output);
			DataOutputStream os = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(output)));
			
			os.write(bytes.size());
			for (byte[] bArr : bytes) {
				os.writeInt(bArr.length);
			}
			for (byte[] bArr : bytes) {
				os.write(bArr);
			}
			os.close();
			
			logAfterSave(bytes, output);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void openArray(Path path, Consumer<List<byte[]>> listConsumer) {
		try {
			DataInputStream is = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)));
			int listSize = is.read();
			int[] arrSizes = new int[listSize];
			for (int i = 0; i < listSize; i++) {
				arrSizes[i] = is.readInt();
			}
			List<byte[]> bytes = new ArrayList<>(listSize);
			for (int arrSize : arrSizes) {
				byte[] bArr = new byte[arrSize];
				is.readFully(bArr);
				bytes.add(bArr);
			}
			
			listConsumer.accept(bytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void saveParsed(ByteBuffer byteBuffer, Path output) {
		if (byteBuffer != null) {
			SaveParsedSingleton.get(output).write(byteBuffer);
		} else {
			SaveParsedSingleton.get(output).close();
		}
	}
	
	private static class SaveParsedSingleton {
		private static SaveParsedSingleton instance;
		private OutputStream outputStream;
		
		private SaveParsedSingleton(Path path) {
			try {
				outputStream = Files.newOutputStream(path);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public static SaveParsedSingleton get(Path path) {
			if (instance == null) {
				instance = new SaveParsedSingleton(path);
			}
			return instance;
		}
		
		public void write(ByteBuffer byteBuffer) {
			try {
				outputStream.write(byteBuffer.array());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void close() {
			try {
				outputStream.close();
				instance = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
