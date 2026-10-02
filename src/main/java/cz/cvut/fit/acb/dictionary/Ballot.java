package cz.cvut.fit.acb.dictionary;

/** Where the analogies of a context vote on the byte that follows it, each for the byte its content starts with. */
public interface Ballot {

    void vote(int symbol, int weight);
}
