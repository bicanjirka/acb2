package cz.cvut.fit.acb.dictionary;

/**
 * The funnel of analogies of a context: the dictionary entries whose contexts agree with it beyond
 * chance, in the order of their weights, each with the position of its content. One funnel is refilled
 * at every step of a segment, so it holds primitive arrays that are overwritten in place; only
 * {@link FunnelWalk} changes it, and what a reader is handed is valid until the next fill.
 */
public final class Funnel {

    private final int[] positions;
    private final int[] weights;
    private int size;
    private long totalWeight;

    Funnel(int capacity) {
        this.positions = new int[capacity];
        this.weights = new int[capacity];
    }

    public int size() {
        return this.size;
    }

    /** The position of the content of candidate {@code index}; the content is the text from there on. */
    public int position(int index) {
        return this.positions[index];
    }

    public int weight(int index) {
        return this.weights[index];
    }

    /** The weights of all the candidates added up. */
    public long totalWeight() {
        return this.totalWeight;
    }

    void clear() {
        this.size = 0;
        this.totalWeight = 0;
    }

    void add(int position, int weight) {
        this.positions[this.size] = position;
        this.weights[this.size++] = weight;
        this.totalWeight += weight;
    }
}
