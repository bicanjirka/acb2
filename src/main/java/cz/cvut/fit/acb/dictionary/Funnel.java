package cz.cvut.fit.acb.dictionary;

/**
 * The funnel of analogies of a context: the dictionary entries whose contexts agree with it beyond
 * chance, each with the position of its content and the byte that starts it, in the order of their weights
 * unless the dictionary says otherwise. One funnel is refilled
 * at every step of a segment, so it holds primitive arrays that are overwritten in place; only
 * {@link FunnelWalk} changes it, and what a reader is handed is valid until the next fill.
 */
public final class Funnel {

    private final int[] positions;
    private final int[] firstBytes;
    private final int[] weights;
    private final int[] agreements;
    private final int[] distances;
    private int size;
    private long totalWeight;

    Funnel(int capacity) {
        this.positions = new int[capacity];
        this.firstBytes = new int[capacity];
        this.weights = new int[capacity];
        this.agreements = new int[capacity];
        this.distances = new int[capacity];
    }

    public int size() {
        return this.size;
    }

    /** The position of the content of candidate {@code index}; the content is the text from there on. */
    public int position(int index) {
        return this.positions[index];
    }

    /** The byte the content of candidate {@code index} starts with, which the dictionary keeps with it. */
    public int firstByte(int index) {
        return this.firstBytes[index];
    }

    public int weight(int index) {
        return this.weights[index];
    }

    /** How many bits the context of candidate {@code index} has in common with the current one. */
    public int agreement(int index) {
        return this.agreements[index];
    }

    /**
     * How many ranks candidate {@code index} lies from the place of the current context: {@code k} for the
     * {@code k}-th entry above it, {@code -k} for the {@code k}-th below.
     */
    public int distance(int index) {
        return this.distances[index];
    }

    /** The weights of all the candidates added up. */
    public long totalWeight() {
        return this.totalWeight;
    }

    void clear() {
        this.size = 0;
        this.totalWeight = 0;
    }

    void add(int position, int firstByte, int weight, int agreement, int distance) {
        this.positions[this.size] = position;
        this.firstBytes[this.size] = firstByte;
        this.agreements[this.size] = agreement;
        this.distances[this.size] = distance;
        this.weights[this.size++] = weight;
        this.totalWeight += weight;
    }
}
