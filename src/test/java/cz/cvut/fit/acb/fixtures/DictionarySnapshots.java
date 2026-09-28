package cz.cvut.fit.acb.fixtures;

import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.dictionary.DictionaryInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Snapshots the encoder's dictionary after every update, then checks the decoder's dictionary
 * against each snapshot in turn. Wrap the encoder's dictionaries with {@link #recording} and the
 * decoder's with {@link #verifying}, both through {@link InterceptingProvider}.
 */
public final class DictionarySnapshots {

    private final List<Dictionary> snapshots = new ArrayList<>();
    private int verified;

    public Dictionary recording(Dictionary delegate) {
        return new ObservedDictionary(delegate, this.snapshots::add);
    }

    public Dictionary verifying(Dictionary delegate) {
        return new ObservedDictionary(delegate, actual -> {
            assertThat(this.verified).as("the decoder updates no more often than the encoder")
                    .isLessThan(this.snapshots.size());
            assertThat(actual).as("dictionary after update %d", this.verified)
                    .isEqualTo(this.snapshots.get(this.verified));
            this.verified++;
        });
    }

    public int recorded() {
        return this.snapshots.size();
    }

    public int verified() {
        return this.verified;
    }

    private static final class ObservedDictionary implements Dictionary {

        private final Dictionary delegate;
        private final Consumer<Dictionary> afterUpdate;

        private ObservedDictionary(Dictionary delegate, Consumer<Dictionary> afterUpdate) {
            this.delegate = delegate;
            this.afterUpdate = afterUpdate;
        }

        @Override
        public Dictionary clone() {
            throw new UnsupportedOperationException("Only the wrapped dictionary is cloned");
        }

        @Override
        public byte[] copy(int cnt, int leng) {
            return this.delegate.copy(cnt, leng);
        }

        @Override
        public DictionaryInfo search(int idx) {
            return this.delegate.search(idx);
        }

        @Override
        public DictionaryInfo searchContent(int ctx, int idx) {
            return this.delegate.searchContent(ctx, idx);
        }

        @Override
        public int searchContext(int idx) {
            return this.delegate.searchContext(idx);
        }

        @Override
        public void update(int idx, int count) {
            this.delegate.update(idx, count);
            this.afterUpdate.accept(this.delegate.clone());
        }

        @Override
        public int select(int idx) {
            return this.delegate.select(idx);
        }
    }
}
