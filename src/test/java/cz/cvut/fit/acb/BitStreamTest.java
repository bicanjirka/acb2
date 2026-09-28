package cz.cvut.fit.acb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import cz.cvut.fit.acb.coding.BitStreamOutputStream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BitStreamTest {

	@Test
	void writtenBytesComeOutUnchanged() throws IOException {
		byte[] input = new byte[256];
		new Random(256).nextBytes(input);
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try (BitStreamOutputStream bits = new BitStreamOutputStream(out)) {
			bits.write(input);
		}

		assertThat(out.toByteArray()).isEqualTo(input);
	}
}
