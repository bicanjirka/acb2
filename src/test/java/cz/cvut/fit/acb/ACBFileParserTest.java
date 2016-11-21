package cz.cvut.fit.acb;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import cz.cvut.fit.acb.dictionary.ByteBuilder;
import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletProcessor;
import cz.cvut.fit.acb.triplets.TripletSupplier;
import cz.cvut.fit.acb.utils.ChainBuilder;
import cz.cvut.fit.acb.utils.Chainable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;


/**
 * @author jiri.bican
 */
public class ACBFileParserTest {
	private static final Logger logger = LogManager.getLogger();
	
	static {
		System.setProperty("log4j.configurationFile", "log4j2.xml");
	}
	
	@Test
	public void parallel() throws Exception {
		Path in = Paths.get("src/test/resources/mississippi.txt");
		ACBFileParser io = new ACBFileParser(4);
		Collection<String> s = new CopyOnWriteArrayList<>();
		ChainBuilder.create(io::open)
				.chain((ByteBuffer byteBuffer, Consumer<TripletSupplier> consumer) -> {
					for (byte b : byteBuffer.array()) {
						consumer.accept(visitor -> {
							visitor.set(new TripletFieldId(0, 8), b);
						});
					}
					consumer.accept(null);
				})
				.chain(new Chainable<TripletSupplier, byte[]>() {
					private Consumer<byte[]> consumer;
					private ByteBuilder bb = new ByteBuilder();
					
					@Override
					public void accept(TripletSupplier ts) {
						if (ts == null) {
							logger.info("sending " + bb);
							consumer.accept(bb.array());
							return;
						}
						ts.visit(new TripletProcessor() {
							@Override
							public void set(TripletFieldId fieldId, int value) {
								byte b = (byte) value;
								logger.info("appending " + (char) b);
								bb.append(b);
							}
							
							@Override
							public int get(TripletFieldId fieldId) {
								return 0;
							}
						});
					}
					
					@Override
					public void setConsumer(Consumer<byte[]> consumer) {
						this.consumer = consumer;
					}
				}).end(b -> {
			logger.info("received " + Arrays.toString(b));
			s.add(new String(b));
		}).accept(in);
		logger.info("FINAL s is " + s);
	}
	
}