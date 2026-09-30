package cz.cvut.fit.acb;

import cz.cvut.fit.acb.associative.AssociativeVariant;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Buyanovsky's coder, in the variant its {@link AssociativeVariant} names: it needs no layout of fields,
 * since the probabilities of what it codes come from the funnel of analogies of each step. The distance bits
 * set how many candidates a funnel takes from either side of a context (two to the power of one less),
 * the length bits the longest match, and the context depth how far contexts are compared.
 */
final class AssociativeCoder implements Coder {

    private final AssociativeVariant variant;

    AssociativeCoder(AssociativeVariant variant) {
        this.variant = variant;
    }

    @Override
    public SegmentCoding segmentCoding(CompressionSettings settings) {
        return new AssociativeSegmentCoding(this.variant, settings);
    }

    @Override
    public Optional<LayoutCoder> layoutCoder() {
        return Optional.empty();
    }

    /** Its models are its own coding of the fields, so no other entropy coding applies. */
    @Override
    public Set<EntropyCoding> entropyCodings() {
        return EnumSet.of(EntropyCoding.ADAPTIVE_ARITHMETIC);
    }

    @Override
    public CompressionSettings preset(CompressionSettings defaults) {
        return defaults.withLengthBits(8).withContextDepth(CompressionSettings.MAX_CONTEXT_DEPTH);
    }
}
