package cz.cvut.fit.acb.dictionary;

/** Funnels written out by hand, for tests of what is done with one. */
public final class FunnelFixtures {

    private FunnelFixtures() {
    }

    /** A funnel of the given positions and weights, in that order. */
    public static Funnel funnelOf(int[] positions, int[] weights) {
        Funnel funnel = new Funnel(positions.length);
        for (int i = 0; i < positions.length; i++) {
            funnel.add(positions[i], weights[i], 0, i + 1);
        }
        return funnel;
    }
}
