package cz.cvut.fit.acb.harness;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.zip.CRC32;

/**
 * One build of the coders behind a {@link BuildClassLoader} of its own, driven by reflection through
 * {@code Compressor}, {@code CompressionSettings.defaultsFor}, {@code TripletCoding} and
 * {@code ContainerFormat.encode}, so that any build that has those can be set beside any other. A round is
 * timed by the CPU time of the calling thread, on which {@code Compressor} codes by default; on Windows that
 * clock moves in steps of about 16 ms, so a round is timed as a whole, never a file at a time.
 */
final class IsolatedBuild {

    private static final ThreadMXBean THREADS = ManagementFactory.getThreadMXBean();

    /** One round over the corpus: its times, and the size and CRC of all its containers. */
    record Round(long compressNanos, long decompressNanos, long containerBytes, long crc) {
    }

    private final Object compressor;
    private final Method compress;
    private final Method decompress;
    private final Method encode;

    private IsolatedBuild(Object compressor, Method compress, Method decompress, Method encode) {
        this.compressor = compressor;
        this.compress = compress;
        this.decompress = decompress;
        this.encode = encode;
    }

    /** @param coder the name of a {@code TripletCoding} constant, in any case */
    static IsolatedBuild load(Path classes, String coder) throws IOException, ReflectiveOperationException {
        ClassLoader loader = new BuildClassLoader(classes, IsolatedBuild.class.getClassLoader());
        Class<?> coding = loader.loadClass("cz.cvut.fit.acb.TripletCoding");
        Class<?> settingsType = loader.loadClass("cz.cvut.fit.acb.CompressionSettings");
        Class<?> compressorType = loader.loadClass("cz.cvut.fit.acb.Compressor");
        Class<?> stream = loader.loadClass("cz.cvut.fit.acb.format.CompressedStream");
        Object constant = coding.getMethod("valueOf", String.class).invoke(null, coder.toUpperCase(Locale.ROOT));
        Object settings = settingsType.getMethod("defaultsFor", coding).invoke(null, constant);
        return new IsolatedBuild(compressorType.getConstructor(settingsType).newInstance(settings),
                compressorType.getMethod("compress", byte[].class), compressorType.getMethod("decompress", stream),
                loader.loadClass("cz.cvut.fit.acb.format.ContainerFormat").getMethod("encode", stream));
    }

    /** Compresses every input, then decompresses every one, and checks each came back as it was. */
    Round round(List<byte[]> inputs) {
        List<Object> streams = new ArrayList<>(inputs.size());
        long start = THREADS.getCurrentThreadCpuTime();
        for (byte[] input : inputs) {
            streams.add(invoke(this.compress, this.compressor, input));
        }
        long middle = THREADS.getCurrentThreadCpuTime();
        List<Object> outputs = new ArrayList<>(inputs.size());
        for (Object stream : streams) {
            outputs.add(invoke(this.decompress, this.compressor, stream));
        }
        long end = THREADS.getCurrentThreadCpuTime();
        CRC32 crc = new CRC32();
        long containerBytes = 0;
        for (int i = 0; i < inputs.size(); i++) {
            if (!Arrays.equals((byte[]) outputs.get(i), inputs.get(i))) {
                throw new IllegalStateException("A round trip changed input " + i);
            }
            byte[] container = (byte[]) invoke(this.encode, null, streams.get(i));
            crc.update(container);
            containerBytes += container.length;
        }
        return new Round(middle - start, end - middle, containerBytes, crc.getValue());
    }

    /** @param target {@code null} for a static method */
    private static Object invoke(Method method, Object target, Object argument) {
        try {
            return method.invoke(target, argument);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("The build failed in " + method.getName(), e.getCause());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
