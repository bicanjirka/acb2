package cz.cvut.fit.acb;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 * @author jiri.bican
 */
//@RunWith(Parameterized.class)
public class ACBFileIOTest {

//	private Path path;
	
	//	@Parameterized.Parameters
	public static Collection<Path[]> data() throws IOException {
		File f = new File("src/t2b/resources/in");
		File[] array = f.listFiles(File::isFile);
		if (array == null)
			fail();
		return Arrays.stream(array).map(File::toPath).map(path1 -> new Path[]{path1}).collect(Collectors.toList());
	}

//	public ACBFileIOTest(Path path) {
//		this.path = path;
//	}
	
	@Test
	public void openParallel() throws Exception {
		
	}
	
	@Test
	public void openParseCount() throws Exception {
		File f = new File("src/test/resources/in");
		File[] array = f.listFiles(File::isFile);
		if (array == null)
			fail();
		for (File file : array) {
			try {
				openParseCount(file, -1);
				fail();
			} catch (IllegalArgumentException e) {
				// expected
			}
			openParseCount(file, 1);
			openParseCount(file, 2);
			openParseCount(file, 13);
			openParseCount(file, 1000);
			openParseCount(file, 1000000);
			openParseCount(file, Integer.MAX_VALUE);
		}
	}
	
	private void openParseCount(File file, int bufferSize) throws Exception {
		assertTrue(file.exists());
		Path path = file.toPath();
		ACBFileIO io = new ACBFileIO(bufferSize);
		final int[] count = {0};
		io.openParse(path, byteBuffer -> {
			if (byteBuffer != null) count[0]++;
		});
		int expCount = (int) Math.ceil((double) Files.size(path) / (double) bufferSize);
		assertEquals(expCount, count[0]);
	}
	
	@Test
	public void openParseSize() throws Exception {
		File f = new File("src/test/resources/in");
		File[] array = f.listFiles(File::isFile);
		if (array == null)
			fail();
		int[] bufferSizes = new int[]{};
		for (File file : array) {
			try {
				openParseSize(file, -1);
				fail();
			} catch (IllegalArgumentException e) {
				// expected
			}
			openParseSize(file, 1);
			openParseSize(file, 2);
			openParseSize(file, 13);
			openParseSize(file, 1000);
			openParseSize(file, 1000000);
			openParseSize(file, Integer.MAX_VALUE);
		}
	}
	
	private void openParseSize(File file, int bufferSize) throws Exception {
		assertTrue(file.exists());
		Path path = file.toPath();
		final boolean[] last = {true};
		ACBFileIO io = new ACBFileIO(bufferSize);
		io.openParse(path, byteBuffer -> {
			if (byteBuffer == null) return;
			if (byteBuffer.array().length < bufferSize) {
				assertTrue(last[0]);
				last[0] = false;
			} else {
				assertEquals(bufferSize, byteBuffer.array().length);
			}
		});
	}
	
	@Test
	public void openSaveObject() throws Exception {
		Path path = Paths.get("src/test/resources/in");
		if (!path.toFile().exists()) {
			Files.createDirectory(path);
		}
		openSaveObject(path, 0);
		openSaveObject(path, 1, 1, 1);
		openSaveObject(path, 10, 20);
		openSaveObject(path, (new Random()).ints(50, 0, 1024).toArray());
		openSaveObject(path, Short.MAX_VALUE);
	}
	
	private void openSaveObject(Path path, int... sizes) throws IOException {
		ACBFileIO io = new ACBFileIO();
		Path path1 = path.resolve("openSaveObject" + Integer.toHexString(io.hashCode()));
		List<byte[]> list = createList(sizes);
		io.saveObject(list, path1);
		io.openObject(path1, bytes -> assertTrue(Arrays.deepEquals(list.toArray(), bytes.toArray())));
		Files.deleteIfExists(path1);
	}
	
	private List<byte[]> createList(int... sizes) {
		List<byte[]> list = new ArrayList<>();
		Random t = new Random();
		for (int i : sizes) {
			byte[] b = new byte[i];
			t.nextBytes(b);
			list.add(b);
		}
		return list;
	}
	
	@Test
	public void openSaveArray() throws Exception {
		Path path = Paths.get("src/test/resources/in");
		openSaveArray(path, 0);
		openSaveArray(path, 1, 1, 1);
		openSaveArray(path, 10, 20);
		openSaveArray(path, (new Random()).ints(50, 0, 1024).toArray());
		openSaveArray(path, Short.MAX_VALUE);
	}
	
	private void openSaveArray(Path path, int... sizes) throws IOException {
		ACBFileIO io = new ACBFileIO();
		Path path1 = path.resolve("openSaveObject" + Integer.toHexString(io.hashCode()));
		List<byte[]> list = createList(sizes);
		io.saveArray(list, path1);
		io.openArray(path1, bytes -> assertTrue(Arrays.deepEquals(list.toArray(), bytes.toArray())));
		Files.deleteIfExists(path1);
	}
}