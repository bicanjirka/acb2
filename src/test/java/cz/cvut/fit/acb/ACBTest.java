package cz.cvut.fit.acb;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import cz.cvut.fit.acb.utils.ACBTestDictionary;
import cz.cvut.fit.acb.utils.ACBTestTripletChecker;
import cz.cvut.fit.acb.utils.ACBTestValueStorage;
import cz.cvut.fit.acb.utils.ACBTestWrapperProvider;
import cz.cvut.fit.acb.utils.ChainBuilder;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

/**
 * @author jiri.bican
 */
//@RunWith(Parameterized.class)
public class ACBTest {
	
	public void testDictionaryConsistency(ACBProvider provider) throws Exception {
		File f = new File("src/test/resources/in");
		File[] array = f.listFiles(File::isFile);
		if (array == null)
			fail();
		for (File file : array) {
			ACBTestWrapperProvider prov = new ACBTestWrapperProvider(provider);
			ACBTestValueStorage storage = new ACBTestValueStorage();
			ACBTestTripletChecker tripletChecker = new ACBTestTripletChecker();
			ACBTestDictionary dict = new ACBTestDictionary();
			prov.wrapDictionary(dict::store);
			
			ACBFileIO io = new ACBFileIO();
			ACB acb = new ACB(prov);
			
			ChainBuilder.create(io::openParse)
					.chain(storage::storeByteBuffer)
					.chain(acb::compress)
					.chain(tripletChecker::t2b)
					.chain(prov.getT2BConverter())
					.end(storage::storeByteArray)
					.accept(file.toPath());
			
			prov.wrapDictionary(dict::test);
			
			ChainBuilder.create(prov.getB2TConverter())
					.chain(tripletChecker::b2t)
					.chain(acb::decompress)
					.end(storage::testStoredByteBuffers)
					.accept(storage.readByteArray());
		}
	}
	
	public void testSegmentedExecution(ACBProvider provider) throws Exception {
		File f = new File("src/test/resources/in");
		File[] array = f.listFiles(File::isFile);
		if (array == null)
			fail();
		for (File file : array) {
			int size = (int) file.length();
			testSegmentedExecution(provider, file, 1);
			testSegmentedExecution(provider, file, 13);
			testSegmentedExecution(provider, file, size / 3);
			testSegmentedExecution(provider, file, size / 2);
			testSegmentedExecution(provider, file, size / 2 + 1);
			testSegmentedExecution(provider, file, size - 2);
			testSegmentedExecution(provider, file, size - 1);
			testSegmentedExecution(provider, file, size);
			testSegmentedExecution(provider, file, size + 1);
		}
	}
	
	private void testSegmentedExecution(ACBProvider provider, File file, int segmentSize) throws IOException {
		ACBTestValueStorage storage = new ACBTestValueStorage();
		ACBTestTripletChecker tripletChecker = new ACBTestTripletChecker();
		ACBFileIO io = new ACBFileIO(segmentSize);
		ACB acb = new ACB(provider);
		
		Path out = Paths.get("src/test/resources/out/" + io.hashCode());
		
		ChainBuilder.create(io::openParse)
				.chain(storage::storeByteBuffer)
				.chain(acb::compress)
				.chain(tripletChecker::t2b)
				.chain(provider.getT2BConverter())
				.end(storage::storeByteArray)
				.accept(file.toPath());
		
		ChainBuilder.create(provider.getB2TConverter())
				.chain(tripletChecker::b2t)
				.chain(acb::decompress)
				.chain(storage::testStoredByteBuffers1)
				.end((byteBuffer) -> io.saveParsed(byteBuffer, out))
				.accept(storage.readByteArray());
		
		assertArrayEquals(Files.readAllBytes(file.toPath()), Files.readAllBytes(out));
		out.toFile().deleteOnExit();
//		Files.deleteIfExists(out);
	}
	
}