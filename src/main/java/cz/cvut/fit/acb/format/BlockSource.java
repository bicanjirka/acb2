package cz.cvut.fit.acb.format;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/** The blocks of a stream one after another, so decoding need not hold them all. */
@FunctionalInterface
public interface BlockSource {

    /**
     * @return the next block, or none once the last one is behind
     * @throws MalformedStreamException if the stream is damaged from here on
     */
    Optional<Block> next() throws MalformedStreamException;

    static BlockSource of(List<Block> blocks) {
        Iterator<Block> remaining = List.copyOf(blocks).iterator();
        return () -> remaining.hasNext() ? Optional.of(remaining.next()) : Optional.empty();
    }

    /** Every block that is left. */
    default List<Block> drain() throws MalformedStreamException {
        List<Block> blocks = new ArrayList<>();
        for (Optional<Block> block = this.next(); block.isPresent(); block = this.next()) {
            blocks.add(block.get());
        }
        return blocks;
    }
}
