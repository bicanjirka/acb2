package cz.cvut.fit.acb.fixtures;

import cz.cvut.fit.acb.dictionary.AnalogyDictionary;
import cz.cvut.fit.acb.dictionary.ByteSet;
import cz.cvut.fit.acb.dictionary.DecoderDictionary;
import cz.cvut.fit.acb.dictionary.EncoderDictionary;
import cz.cvut.fit.acb.dictionary.Funnel;
import cz.cvut.fit.acb.dictionary.SearchResult;
import cz.cvut.fit.acb.format.MalformedStreamException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Snapshots the encoder's dictionary (the position at every rank) after every update, then checks
 * the decoder's dictionary against each snapshot in turn. Give the encoder {@link #recording()} and
 * the decoder {@link #verifying()}, both through {@link InterceptingProvider}.
 */
public final class DictionarySnapshots {

    private final List<int[]> snapshots = new ArrayList<>();
    private int verified;

    /** What a provider does with the dictionaries it builds. */
    public interface Side {

        EncoderDictionary observing(EncoderDictionary dictionary);

        DecoderDictionary observing(DecoderDictionary dictionary);

        AnalogyDictionary observing(AnalogyDictionary dictionary);

        /** Leaves every dictionary as it is. */
        static Side ignoring() {
            return new Side() {
                @Override
                public EncoderDictionary observing(EncoderDictionary dictionary) {
                    return dictionary;
                }

                @Override
                public DecoderDictionary observing(DecoderDictionary dictionary) {
                    return dictionary;
                }

                @Override
                public AnalogyDictionary observing(AnalogyDictionary dictionary) {
                    return dictionary;
                }
            };
        }
    }

    public Side recording() {
        return this.side(positions -> this.snapshots.add(positions));
    }

    public Side verifying() {
        return this.side(positions -> {
            assertThat(this.verified).as("the decoder updates no more often than the encoder")
                    .isLessThan(this.snapshots.size());
            assertThat(positions).as("dictionary after update %d", this.verified)
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

    private Side side(Consumer<int[]> afterUpdate) {
        return new Side() {
            @Override
            public EncoderDictionary observing(EncoderDictionary dictionary) {
                return new ObservedEncoder(dictionary, afterUpdate);
            }

            @Override
            public DecoderDictionary observing(DecoderDictionary dictionary) {
                return new ObservedDecoder(dictionary, afterUpdate);
            }

            @Override
            public AnalogyDictionary observing(AnalogyDictionary dictionary) {
                return new ObservedAnalogy(dictionary, afterUpdate);
            }
        };
    }

    /** The position of the entry at a rank, as a dictionary says it. */
    private interface Selector {

        int select(int rank) throws MalformedStreamException;
    }

    private static int[] positionsOf(int size, Selector dictionary) {
        try {
            int[] positions = new int[size];
            for (int rank = 0; rank < positions.length; rank++) {
                positions[rank] = dictionary.select(rank);
            }
            return positions;
        } catch (MalformedStreamException e) {
            throw new AssertionError("Every rank below the size is in the dictionary", e);
        }
    }

    private static final class ObservedEncoder implements EncoderDictionary {

        private final EncoderDictionary delegate;
        private final Consumer<int[]> afterUpdate;

        private ObservedEncoder(EncoderDictionary delegate, Consumer<int[]> afterUpdate) {
            this.delegate = delegate;
            this.afterUpdate = afterUpdate;
        }

        @Override
        public int size() {
            return this.delegate.size();
        }

        @Override
        public int contextRank(int idx) {
            return this.delegate.contextRank(idx);
        }

        @Override
        public ByteSet continuations(int idx, int content, int length) {
            return this.delegate.continuations(idx, content, length);
        }

        @Override
        public SearchResult search(int idx) {
            return this.delegate.search(idx);
        }

        @Override
        public int impliedLength(int idx, int distance) {
            return this.delegate.impliedLength(idx, distance);
        }

        @Override
        public void update(int idx, int count) {
            this.delegate.update(idx, count);
            this.afterUpdate.accept(positionsOf(this.delegate.size(), this.delegate::select));
        }

        @Override
        public int select(int rank) throws MalformedStreamException {
            return this.delegate.select(rank);
        }
    }

    private static final class ObservedDecoder implements DecoderDictionary {

        private final DecoderDictionary delegate;
        private final Consumer<int[]> afterUpdate;

        private ObservedDecoder(DecoderDictionary delegate, Consumer<int[]> afterUpdate) {
            this.delegate = delegate;
            this.afterUpdate = afterUpdate;
        }

        @Override
        public int size() {
            return this.delegate.size();
        }

        @Override
        public int contextRank(int idx) {
            return this.delegate.contextRank(idx);
        }

        @Override
        public ByteSet continuations(int idx, int content, int length) {
            return this.delegate.continuations(idx, content, length);
        }

        @Override
        public int impliedLength(int idx, int distance) throws MalformedStreamException {
            return this.delegate.impliedLength(idx, distance);
        }

        @Override
        public void update(int idx, int count) {
            this.delegate.update(idx, count);
            this.afterUpdate.accept(positionsOf(this.delegate.size(), this.delegate::select));
        }

        @Override
        public int select(int rank) throws MalformedStreamException {
            return this.delegate.select(rank);
        }
    }

    private static final class ObservedAnalogy implements AnalogyDictionary {

        private final AnalogyDictionary delegate;
        private final Consumer<int[]> afterUpdate;

        private ObservedAnalogy(AnalogyDictionary delegate, Consumer<int[]> afterUpdate) {
            this.delegate = delegate;
            this.afterUpdate = afterUpdate;
        }

        @Override
        public int size() {
            return this.delegate.size();
        }

        @Override
        public void update(int idx, int count) {
            this.delegate.update(idx, count);
            this.afterUpdate.accept(positionsOf(this.delegate.size(), this.delegate::select));
        }

        @Override
        public int select(int rank) throws MalformedStreamException {
            return this.delegate.select(rank);
        }

        @Override
        public Funnel funnel(int position) {
            return this.delegate.funnel(position);
        }

        @Override
        public Funnel forecast(int position) {
            return this.delegate.forecast(position);
        }
    }
}
