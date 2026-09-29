package cz.cvut.fit.acb.format;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BlockTest {

    @Test
    void aBlockKeepsTheBytesItWasMadeFromWhateverHappensToThem() {
        byte[] bytes = {1, 2, 3};
        Block block = Block.coded(10, bytes);

        bytes[0] = 99;
        block.bytes()[1] = 99;

        assertThat(block.bytes()).containsExactly(1, 2, 3);
    }

    @Test
    void aStoredBlockDecodesToItsOwnLength() {
        Block block = Block.stored(new byte[]{1, 2, 3});

        assertThat(block.rawLength()).isEqualTo(3);
        assertThat(block.storedLength()).isEqualTo(3);
    }

    @Test
    void aCodedBlockRemembersHowLongItsSegmentIs() {
        Block block = Block.coded(10, new byte[]{1, 2, 3});

        assertThat(block.rawLength()).isEqualTo(10);
        assertThat(block.storedLength()).isEqualTo(3);
    }

    @Test
    void blocksWithTheSameBytesAreEqualButStoredIsNeverCoded() {
        assertThat(Block.coded(5, new byte[]{1})).isEqualTo(Block.coded(5, new byte[]{1}));
        assertThat(Block.stored(new byte[]{1})).isNotEqualTo(Block.coded(1, new byte[]{1}));
    }

    @Test
    void aBlockOfNoBytesCannotBeMade() {
        assertThatThrownBy(() -> Block.stored(new byte[0])).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Block.coded(0, new byte[]{1})).isInstanceOf(IllegalArgumentException.class);
    }
}
