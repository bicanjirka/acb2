package cz.cvut.fit.acb.dictionary;

/** {@link ContextOrder#byLastBytes}. */
final class LastBytesOrder implements ContextOrder {

    private final SegmentBuffer segment;
    private final int depth;

    LastBytesOrder(SegmentBuffer segment, int depth) {
        this.segment = segment;
        this.depth = depth;
    }

    @Override
    public int compare(int first, int second) {
        byte[] bytes = this.segment.bytes();
        int limit = Math.min(Math.min(first, second), this.depth);
        int agreed = ContextAgreement.commonBytes(bytes, first, second, limit);
        if (agreed == limit) {
            return Integer.compare(first, second);
        }
        return Byte.toUnsignedInt(bytes[first - 1 - agreed]) - Byte.toUnsignedInt(bytes[second - 1 - agreed]);
    }

    @Override
    public int followingByte(int position) {
        return Byte.toUnsignedInt(this.segment.bytes()[position]);
    }

    @Override
    public long prefix(int position) {
        return ContextAgreement.nearestBytes(this.segment.bytes(), position,
                Math.min(Math.min(position, this.depth), Long.BYTES));
    }

    @Override
    public int sharedBits(int first, long firstPrefix, int second, long secondPrefix) {
        int limit = Math.min(Math.min(first, second), this.depth);
        long difference = firstPrefix ^ secondPrefix;
        if (difference != 0) {
            return Math.min(limit << 3, Long.numberOfLeadingZeros(difference));
        }
        if (limit <= Long.BYTES) {
            return limit << 3;
        }
        byte[] bytes = this.segment.bytes();
        return ContextAgreement.bitsAfter(bytes, first, second,
                ContextAgreement.commonBytesFrom(bytes, first, second, Long.BYTES, limit), limit);
    }

    @Override
    public int sharedBits(int first, int second) {
        byte[] bytes = this.segment.bytes();
        int limit = Math.min(Math.min(first, second), this.depth);
        return ContextAgreement.bitsAfter(bytes, first, second, ContextAgreement.commonBytes(bytes, first, second, limit),
                limit);
    }
}
