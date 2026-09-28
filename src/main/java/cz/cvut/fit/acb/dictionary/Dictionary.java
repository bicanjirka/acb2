package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.format.MalformedStreamException;

import java.util.Comparator;

public interface Dictionary {

    Dictionary clone();

    /**
     * The first {@code leng} bytes of the content of rank {@code cnt}, repeated if it is shorter.
     *
     * @throws MalformedStreamException if the rank is not in the dictionary
     */
    byte[] copy(int cnt, int leng) throws MalformedStreamException;

    DictionaryInfo search(int idx);

    DictionaryInfo searchContent(int ctx, int idx);

    int searchContext(int idx);

    void update(int idx, int count);

    /**
     * @return the position of the entry with rank {@code idx}
     * @throws MalformedStreamException if the rank is not in the dictionary
     */
    int select(int idx) throws MalformedStreamException;

    class ReverseIndexComparator implements Comparator<Integer> {
        private static final int MAGIC_CONST = 10;
        ByteSequence s;

        public ReverseIndexComparator(ByteSequence s) {
            this.s = s;
        }

        @Override
        public int compare(Integer o1, Integer o2) {
            int len1 = o1;
            int len2 = o2;
            int lim = Math.min(len1, len2);
            lim = Math.min(lim, MAGIC_CONST);

            int k = 1;
            while (k <= lim) {
                byte b1 = s.byteAt(len1 - k);
                byte b2 = s.byteAt(len2 - k);
                if (b1 != b2) {
                    return b1 - b2;
                }
                k++;
            }
            return len1 - len2;
        }
    }
}
