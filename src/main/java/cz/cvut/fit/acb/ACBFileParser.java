/*
 * Copyright 2002-2016 Ataccama Software, s.r.o. All rights reserved.
 * ATACCAMA PROPRIETARY/CONFIDENTIAL.
 * Any use of this source code is prohibited without prior written
 * permission of Ataccama Software, s.r.o.; Czech Republic, Id.no.: 28235550
 * http://www.ataccama.com
 */
package cz.cvut.fit.acb;

import cz.cvut.fit.acb.utils.Chainable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * @author jiri.bican
 */
public class ACBFileParser implements Chainable<Path, ByteBuffer> {
    private static final Logger logger = LogManager.getLogger();
    private static final int BUFFER_UNIT = 1_000_000;
    private Consumer<ByteBuffer> consumer;

    @Override
    public void setConsumer(Consumer<ByteBuffer> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void accept(Path path) {
        try {
            SeekableByteChannel sbc = Files.newByteChannel(path);
            logger.info("Opened file '{}' [size = {}]", path, sbc.size());
            while (sbc.position() < sbc.size()) {
                int size = (int) Math.min(BUFFER_UNIT, sbc.size() - sbc.position());
                ByteBuffer bb = ByteBuffer.allocate(size);
                sbc.read(bb);
                consumer.accept(bb);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
