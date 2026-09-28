package cz.cvut.fit.acb.triplets.coder;

import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.dictionary.DictionaryInfo;

import java.util.function.IntFunction;

/** Answers each search with a scripted match, so a coder's triplet layout can be tested alone. */
final class FakeDictionary implements Dictionary {

    private final IntFunction<DictionaryInfo> matches;

    private FakeDictionary(IntFunction<DictionaryInfo> matches) {
        this.matches = matches;
    }

    static FakeDictionary matching(IntFunction<DictionaryInfo> matches) {
        return new FakeDictionary(matches);
    }

    static DictionaryInfo noMatch() {
        return new DictionaryInfo(0, -1, 0);
    }

    static DictionaryInfo match(int distance, int length) {
        return new DictionaryInfo(distance, 0, length);
    }

    @Override
    public DictionaryInfo search(int idx) {
        return this.matches.apply(idx);
    }

    @Override
    public void update(int idx, int count) {
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("Encoding never asks the size");
    }

    @Override
    public DictionaryInfo searchContent(int ctx, int idx) {
        throw new UnsupportedOperationException("Encoding searches through search");
    }

    @Override
    public int searchContext(int idx) {
        throw new UnsupportedOperationException("Encoding searches through search");
    }

    @Override
    public int select(int idx) {
        throw new UnsupportedOperationException("Encoding never selects");
    }
}
