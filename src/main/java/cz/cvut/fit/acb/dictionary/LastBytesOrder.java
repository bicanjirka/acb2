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
    public long prefix(int position) {
        return ContextAgreement.nearestBytes(this.segment.bytes(), position,
                Math.min(Math.min(position, this.depth), Long.BYTES));
    }

    @Override
    public int sharedBytes(int first, long firstPrefix, int second, long secondPrefix) {
        int limit = Math.min(Math.min(first, second), this.depth);
        long difference = firstPrefix ^ secondPrefix;
        if (difference != 0) {
            return Math.min(limit, Long.numberOfLeadingZeros(difference) >>> 3);
        }
        return limit <= Long.BYTES ? limit
                : ContextAgreement.commonBytesFrom(this.segment.bytes(), first, second, Long.BYTES, limit);
    }

    @Override
    public int sharedBytes(int first, int second) {
        return ContextAgreement.commonBytes(this.segment.bytes(), first, second,
                Math.min(Math.min(first, second), this.depth));
    }
}
