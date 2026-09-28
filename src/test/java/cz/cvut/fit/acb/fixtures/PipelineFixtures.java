package cz.cvut.fit.acb.fixtures;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import cz.cvut.fit.acb.ACB;
import cz.cvut.fit.acb.ACBProvider;
import cz.cvut.fit.acb.utils.ChainBuilder;

/**
 * The production compress and decompress chains, fed from and collected into memory instead of
 * files. Segments are cut the way {@code ACBFileIO.openParse} cuts a file.
 */
public final class PipelineFixtures {

	/** Larger than every corpus file, so the whole input is one segment. */
	public static final int WHOLE_INPUT = 1_000_000;

	private PipelineFixtures() {
	}

	public static byte[] roundTrip(ACBProvider provider, byte[] input, int segmentSize) {
		TripletLog log = new TripletLog();
		return decompress(provider, compress(provider, input, segmentSize, log), log);
	}

	public static List<byte[]> compress(ACBProvider provider, byte[] input, int segmentSize, TripletLog log) {
		return compress(new ACB(provider), provider, input, segmentSize, log);
	}
	
	/** Compresses with a caller-owned {@code acb}, so a test can reuse one across streams. */
	public static List<byte[]> compress(ACB acb, ACBProvider provider, byte[] input, int segmentSize,
	                                    TripletLog log) {
		AtomicReference<List<byte[]>> compressed = new AtomicReference<>();
		ChainBuilder.create(segmentsOf(segmentSize))
				.chain(acb::compress)
				.chain(log::recordWrites)
				.chain(provider.getT2BConverter())
				.end(compressed::set)
				.accept(input);
		return compressed.get();
	}

	public static byte[] decompress(ACBProvider provider, List<byte[]> compressed, TripletLog log) {
		ByteArrayOutputStream decoded = new ByteArrayOutputStream();
		ACB acb = new ACB(provider);
		ChainBuilder.create(provider.getB2TConverter())
				.chain(log::checkReads)
				.chain(acb::decompress)
				.end(segment -> {
					if (segment != null) {
						decoded.writeBytes(segment.array());
					}
				})
				.accept(compressed);
		return decoded.toByteArray();
	}

	private static BiConsumer<byte[], Consumer<ByteBuffer>> segmentsOf(int segmentSize) {
		return (input, next) -> {
			for (long from = 0; from < input.length; from += segmentSize) {
				int to = (int) Math.min(input.length, from + segmentSize);
				next.accept(ByteBuffer.wrap(Arrays.copyOfRange(input, (int) from, to)));
			}
			next.accept(null);
		};
	}
}
