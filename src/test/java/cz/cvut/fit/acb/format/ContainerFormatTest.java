package cz.cvut.fit.acb.format;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;

import cz.cvut.fit.acb.EntropyCoding;
import cz.cvut.fit.acb.TripletCoding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContainerFormatTest {
	
	private static final StreamHeader HEADER =
			new StreamHeader(6, 4, TripletCoding.VALACH, EntropyCoding.BIT_ARRAY, new int[]{45, 13, 10});
	private static final List<byte[]> PAYLOAD = List.of(new byte[]{0, 0, 0, 7}, new byte[0], new byte[]{-1, 1, -128});
	
	@ParameterizedTest
	@EnumSource(TripletCoding.class)
	void aStreamDecodesToTheHeaderAndPayloadItWasEncodedFrom(TripletCoding tripletCoding) throws MalformedStreamException {
		StreamHeader header = new StreamHeader(9, 3, tripletCoding, EntropyCoding.ADAPTIVE_ARITHMETIC, new int[]{2, 1});
		
		CompressedStream decoded = ContainerFormat.decode(ContainerFormat.encode(new CompressedStream(header, PAYLOAD)));
		
		assertThat(decoded.header()).isEqualTo(header);
		assertThat(decoded.payload()).containsExactlyElementsOf(PAYLOAD);
	}
	
	@Test
	void flippingAnyBitIsReportedAsMalformed() {
		byte[] encoded = encoded();
		
		for (int bit = 0; bit < encoded.length * Byte.SIZE; bit++) {
			byte[] corrupt = encoded.clone();
			corrupt[bit / Byte.SIZE] ^= (byte) (1 << (bit % Byte.SIZE));
			
			assertThatThrownBy(() -> ContainerFormat.decode(corrupt)).as("bit %d", bit)
					.isInstanceOf(MalformedStreamException.class);
		}
	}
	
	@Test
	void everyTruncationIsReportedAsMalformed() {
		byte[] encoded = encoded();
		
		for (int length = 0; length < encoded.length; length++) {
			byte[] truncated = Arrays.copyOf(encoded, length);
			
			assertThatThrownBy(() -> ContainerFormat.decode(truncated)).as("length %d", length)
					.isInstanceOf(MalformedStreamException.class);
		}
	}
	
	@Test
	void aJavaSerializedFileFromBeforeTheFormatIsNotAnAcbStream() {
		byte[] javaSerialization = {(byte) 0xAC, (byte) 0xED, 0x00, 0x05, 0x73, 0x72};
		
		assertThatThrownBy(() -> ContainerFormat.decode(javaSerialization))
				.isInstanceOf(MalformedStreamException.class).hasMessage("Not an ACB stream");
	}
	
	@Test
	void anotherVersionIsRejectedByName() {
		byte[] encoded = encoded();
		encoded[3] = (byte) (ContainerFormat.VERSION + 1);
		
		assertThatThrownBy(() -> ContainerFormat.decode(withChecksum(encoded)))
				.isInstanceOf(MalformedStreamException.class).hasMessageContaining("version");
	}
	
	@Test
	void anUnknownCoderCodeWithAValidChecksumIsRejected() {
		byte[] encoded = encoded();
		encoded[6] = 99;
		
		assertThatThrownBy(() -> ContainerFormat.decode(withChecksum(encoded)))
				.isInstanceOf(MalformedStreamException.class).hasMessageContaining("triplet coding code 99");
	}
	
	@Test
	void aLengthLargerThanTheStreamIsRejectedWithoutAllocatingIt() {
		byte[] encoded = encoded();
		int arrayCountOffset = 8 + Integer.BYTES + 3 * Integer.BYTES;
		ByteBuffer.wrap(encoded).putInt(arrayCountOffset + Integer.BYTES, Integer.MAX_VALUE);
		
		assertThatThrownBy(() -> ContainerFormat.decode(withChecksum(encoded)))
				.isInstanceOf(MalformedStreamException.class).hasMessageContaining("byte count");
	}
	
	private static byte[] encoded() {
		return ContainerFormat.encode(new CompressedStream(HEADER, PAYLOAD));
	}
	
	/** Re-seals edited bytes, so a test reaches the check behind the checksum. */
	private static byte[] withChecksum(byte[] encoded) {
		CRC32 crc = new CRC32();
		crc.update(encoded, 0, encoded.length - Integer.BYTES);
		ByteBuffer.wrap(encoded).putInt(encoded.length - Integer.BYTES, (int) crc.getValue());
		return encoded;
	}
}
